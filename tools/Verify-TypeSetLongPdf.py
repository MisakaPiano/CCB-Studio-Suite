"""Independent product PDF geometry, embedded raster and Poppler rendering verification; no network."""
import hashlib, json, subprocess
from pathlib import Path
import pypdf
import pypdfium2 as pdfium
from PIL import Image, ImageChops

root=Path(__file__).resolve().parents[1]
spec=json.loads((root/'modules/studio-core/build/typeset-long-pdf/latest.json').read_text(encoding='utf-8'))
directory=Path(spec['directory']);output=root/'modules/studio-core/build/typeset-long-pdf/independent'
output.mkdir(parents=True,exist_ok=True)
report={'status':'RUNNING','pypdf':pypdf.__version__,'pdfium':str(pdfium.PDFIUM_INFO),'pdfSha256':hashlib.sha256((directory/'book.pdf').read_bytes()).hexdigest(),'pages':[]}
try:
    version=subprocess.run(['pdftoppm','-v'],capture_output=True,text=True,check=True)
    report['poppler']=version.stderr.strip()
    reader=pypdf.PdfReader(directory/'book.pdf',strict=True)
    assert reader.pdf_header=='%PDF-2.0'
    pdfium_doc=pdfium.PdfDocument(directory/'book.pdf')
    assert len(reader.pages)==len(spec['pixels'])==2
    for i,page in enumerate(reader.pages):
        expected=tuple(spec['pixels'][i]);unit=float(page.get('/UserUnit',1))
        raw=[float(page.mediabox.width),float(page.mediabox.height)]
        physical=[v*unit for v in raw]
        entry={'page':i+1,'pixels':expected,'userUnit':unit,'mediaBox':raw,'points':physical};report['pages'].append(entry)
        assert all(abs(p-expected[j]*.75)<1e-6 for j,p in enumerate(physical))
        assert unit==1
        original=Image.open(directory/f'{i+1:03}.png').convert('RGB')
        embedded=page.images[0].image.convert('RGB')
        assert embedded.size==expected and ImageChops.difference(original,embedded).getbbox() is None
        entry['embeddedRaster']='EXACT_RGB'
        prefix=output/f'page-{i+1}'
        command=['pdftoppm','-r','96','-f',str(i+1),'-l',str(i+1),'-singlefile','-png',str(directory/'book.pdf'),str(prefix)]
        result=subprocess.run(command,capture_output=True,text=True,check=True)
        entry['renderCommand']=command;entry['renderStderr']=result.stderr
        rendered=Image.open(prefix.with_suffix('.png')).convert('RGB');entry['renderPixels']=rendered.size
        assert rendered.size==expected
        entry['renderPixelExact']=ImageChops.difference(original,rendered).getbbox() is None
        # Poppler applies image smoothing even to an exact-size embedded raster. Keep that fact,
        # and independently require exact decoded/rasterized pixels using PDFium's no-smoothing mode.
        pdfium_page=pdfium_doc[i]
        try:
            assert all(abs(v-physical[j])<1e-6 for j,v in enumerate(pdfium_page.get_size()))
            bitmap=pdfium_page.render(scale=4/3,no_smoothimage=True)
            try:
                exact=bitmap.to_pil().convert('RGB')
                entry['pdfiumPixels']=exact.size
                assert exact.size==expected and ImageChops.difference(original,exact).getbbox() is None
                entry['pdfiumRaster']='EXACT_RGB'
            finally:bitmap.close()
        finally:pdfium_page.close()
        if i==1:
            rendered.crop((0,0,expected[0],600)).save(output/'long-top.png')
            rendered.crop((0,expected[1]-600,expected[0],expected[1])).save(output/'long-bottom.png')
    pdfium_doc.close()
    report['status']='PASS'
except Exception as error:
    report['status']='FAILED';report['error']=type(error).__name__+': '+str(error)
    raise
finally:
    (output/'verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
