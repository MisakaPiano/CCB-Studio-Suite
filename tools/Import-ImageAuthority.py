"""Reproduce the approved exact-source closure. Never modifies the reference repo."""
import argparse, hashlib, json, re, subprocess
from pathlib import Path

SHA = '5ef95428421f7c693b010ac88f51d470804ab07a'
PREFIX = 'app/sharedCore/src/main/kotlin/'
SEEDS = ['domain/prompt/NovelAiPromptAuthority.kt', 'domain/image/NovelAiPromptDesigner.kt',
         'domain/image/NovelAiImageService.kt', 'domain/image/NovelAiStudioModels.kt',
         'domain/image/NovelAiPngMetadataReader.kt', 'domain/image/NovelAiStudioImageImport.kt',
         'domain/image/PrivacyPngEncoder.kt', 'domain/image/ImageMetadataStripper.kt',
         'domain/chat/OpenAiChatRequestSerializer.kt', 'domain/chat/OpenAiCompletionResponsePolicy.kt',
         'domain/image/NovelAiTagLookup.kt', 'domain/image/NovelAiCatalogQueries.kt',
         'domain/update/SharedDanbooruCatalogUpdateChecker.kt', 'domain/image/NovelAiRankedIndexBuilder.kt',
         'domain/image/NovelAiTagSuggestionService.kt', 'domain/image/NovelAiPromptWordDictionary.kt',
         'domain/image/NovelAiPromptTranslation.kt', 'domain/image/NovelAiPromptTokenCounter.kt',
         'domain/image/RankedTagCompletion.kt', 'domain/image/NovelAiVibeEncodingCore.kt',
         'domain/image/NovelAiFocusedInpaintPlanner.kt', 'domain/image/NovelAiUpscaleService.kt',
         'domain/image/ImageCanvasHistory.kt', 'domain/image/NovelAiFocusedMaskPixels.kt',
         'domain/image/NovelAiPixelResampler.kt', 'domain/image/ImageCropMath.kt',
         'domain/image/NovelAiInpaintPixelPolicy.kt', 'domain/card/CardTransferModels.kt',
         'domain/card/PngTextChunks.kt']
TESTS = ['NovelAiDesignerTransportTest', 'NovelAiImageModelResolutionTest',
         'NovelAiImageGuidanceTest', 'NovelAiStudioRequestTest', 'NovelAiImageRetryTest',
         'NovelAiPngMetadataReaderTest', 'NovelAiV5TextPromptPolicyTest',
         'NovelAiCharacterPositionTest', 'NovelAiCharacterEnabledTest',
         'NovelAiStudioCustomSizeTest', 'NovelAiPromptPostProcessorTest',
         'PrivacyPngEncoderTest', 'StealthAlphaMetadataTest', 'NovelAiTagCompletionTest',
         'NovelAiTagSuggestionServiceTest', 'NovelAiTagResearchServiceTest']

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-repo', required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    def git(*args):
        return subprocess.check_output(['git', '--no-optional-locks', '-C', str(Path(args_repo)), *args])
    args_repo = args.source_repo
    paths = git('ls-tree', '-r', '--name-only', SHA, PREFIX).decode().splitlines()
    # Declaration-only index; exact source bytes are read only when in the closure.
    index = git('grep', '-n', '-E', r'^(public |internal |private |data |sealed |enum |value |open |abstract |inline |suspend )*(class|interface|object|fun|typealias|val|const val) ', SHA, '--', PREFIX).decode('utf-8')
    symbols = {}
    for line in index.splitlines():
        _, path, _, declaration = line.split(':', 3)
        match = re.search(r'\b(?:class|interface|object|typealias|fun|val)\s+(?:<[^>]+>\s*)?(?:[\w<>?]+\.)?(\w+)', declaration)
        if match:
            package = path[len(PREFIX):].rsplit('/', 1)[0].replace('/', '.')
            symbols.setdefault(package+'.'+match[1], set()).add(path)
    chosen, reasons, contents = set(), {}, {}
    queue = [(PREFIX+'com/example/chatbar/'+p, 'approved image authority consumer') for p in SEEDS]
    test_paths = ['app/sharedCore/src/test/kotlin/com/example/chatbar/domain/image/'+n+'.kt' for n in TESTS]
    queue += [(p, 'upstream semantic regression') for p in test_paths]
    while queue:
        path, reason = queue.pop(0)
        reasons.setdefault(path, set()).add(reason)
        if path in chosen: continue
        chosen.add(path)
        raw = git('show', SHA+':'+path); contents[path] = raw
        body = raw.decode('utf-8')
        package = re.search(r'^package (\S+)', body, re.M)[1]
        imports = re.findall(r'^import ([\w.*]+)', body, re.M)
        tokens = set(re.findall(r'\b[A-Za-z_]\w*\b', body))
        refs = {package+'.'+t for t in tokens}
        for imp in imports:
            if imp.endswith('.*'): refs.update(imp[:-1]+t for t in tokens)
            else:
                refs.add(imp)
                # import of member/extension under a class
                parts = imp.split('.')
                refs.update('.'.join(parts[:i]) for i in range(4,len(parts)))
        for ref in sorted(refs):
            for target in symbols.get(ref, []):
                if target != path: queue.append((target, path[len('app/sharedCore/'):]))
    resource = 'app/sharedCore/src/test/resources/fixtures/novelai_v45_precise_payload.json'
    chosen.add(resource); contents[resource]=git('show',SHA+':'+resource); reasons[resource]={'upstream golden fixture'}
    manifest = []
    for source in sorted(chosen):
        rel = source[len('app/sharedCore/'):]
        target = root/'modules/image-authority'/rel
        raw = contents[source]
        if target.exists() and target.read_bytes()!=raw: raise RuntimeError('Refuse to overwrite changed '+str(target))
        target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(raw)
        manifest.append(dict(source=source,path=target.relative_to(root).as_posix(),size=len(raw),sha256=hashlib.sha256(raw).hexdigest(),reasons=sorted(reasons[source])))
    lock=dict(repository='MisakaPiano/ChatChatBar-Desktop',commit=SHA,byteMode='unmodified',rightsEvidence='USER-DIRECT; Issue #1 comment6299072689, not independently verified original grant',files=manifest)
    (root/'modules/image-authority/SOURCE_MANIFEST.json').write_text(json.dumps(lock,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(files=len(manifest),bytes=sum(x['size'] for x in manifest),paths=[x['path'] for x in manifest]),indent=2))

if __name__=='__main__': main()
