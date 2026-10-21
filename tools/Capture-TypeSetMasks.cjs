/* Read-only TypeSet source execution; isolated new Edge context, all network blocked. */
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'.local/typeset/masks',new Date().toISOString().replace(/[:.]/g,'-'));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
fs.mkdirSync(out,{recursive:true});let browser;
(async()=>{
 const raw=fs.readFileSync(path.join(root,'.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html'));
 if(sha(raw)!=='892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63')throw Error('Source identity');
 let html=raw.toString('utf8');const a=html.indexOf('const DEFAULT_PREFS = {'),b=html.indexOf('\n};',a)+3,defaults=html.slice(a,b);
 html=html.slice(0,a)+html.slice(b);html=html.replace('function defaultStorybookConfig(){',defaults+'\nfunction defaultStorybookConfig(){');
 html=html.replace('</script>','window.__maskReference={state,drawMask,samplePolylineUniform};</script>');
 const log={sourceSha256:sha(raw),instrumentedSha256:sha(html),instrumentation:'Only known initialization-order correction and test hook; unchanged mask algorithms',cases:[],blocked:[],human:'NOT TESTED'};
 browser=await chromium.launch({channel:'msedge',headless:true,args:['--disable-gpu','--disable-extensions','--no-first-run']});log.browser=browser.version();
 const page=await browser.newPage();await page.route('**/*',r=>r.request().url()==='https://typeset.invalid/'?r.fulfill({contentType:'text/html',body:html}):(log.blocked.push(r.request().url()),r.abort()));await page.goto('https://typeset.invalid/');
 await page.waitForFunction(()=>!!window.__maskReference);
 const fixture=await page.evaluate(async()=>{const c=document.createElement('canvas');c.width=180;c.height=120;const g=c.getContext('2d');const d=g.createImageData(180,120);for(let y=0;y<120;y++)for(let x=0;x<180;x++){const i=(y*180+x)*4;d.data[i]=x%32*8;d.data[i+1]=y%32*8;d.data[i+2]=(x+y)%32*8;d.data[i+3]=255;}g.putImageData(d,0,0);const image=new Image();image.src=c.toDataURL();await image.decode();window.__maskReference.state.image=image;return c.toDataURL().split(',')[1];});
 fs.writeFileSync(path.join(out,'original.png'),Buffer.from(fixture,'base64'));
 for(const shape of ['rect','brush','lasso'])for(const type of ['black','white','mosaic','blur']){
  const m={shape,type,x:30,y:30,w:100,h:60,size:34,strength:12,density:6,stripeThickness:6,pointDensity:8,points:[{x:30,y:30},{x:90,y:70},{x:140,y:35},{x:120,y:95}]};
  const result=await page.evaluate(m=>{const r=window.__maskReference,c=document.createElement('canvas');c.width=180;c.height=120;const g=c.getContext('2d');g.drawImage(r.state.image,0,0);r.drawMask(g,m);return {png:c.toDataURL().split(',')[1],samples:r.samplePolylineUniform(m.points,100/m.density)};},m);
  const name=shape+'-'+type;const bytes=Buffer.from(result.png,'base64');fs.writeFileSync(path.join(out,name+'.png'),bytes);log.cases.push({name,mask:m,samples:result.samples,bytes:bytes.length,sha256:sha(bytes)});
 }
 fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify(log,null,2));console.log(out);await browser.close();
})().catch(async e=>{console.error(e);if(browser)await browser.close();process.exitCode=1;});
