# Bundled local region detector

- Publisher / model repository: 01miku, https://huggingface.co/01miku/anime-nsfw-segm-yolo26
- Fixed upstream revision: `1697d5d1827b6a818b350b44bf3ec27f08837a2a`
- Model: `nsfw-anime-medium-x1280.onnx`, 47,600,269 bytes
- SHA-256 / upstream LFS object ID: `a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828`
- Exact model card: https://huggingface.co/01miku/anime-nsfw-segm-yolo26/blob/1697d5d1827b6a818b350b44bf3ec27f08837a2a/README.md
- Declared license in that card: MIT. A separate LICENSE file was not present at the inspected revision. No additional upstream copyright line was provided; none is invented here. The MIT permission text is reproduced in `MIT.txt`, and the original declaration is preserved below.

Original model card (verbatim):

```text
---
license: mit
---

model for detecting nsfw areas in images for detailing, censoring or de-censoring.

objects: nipple, vagina, penis, anus, pubic hair, female face & male face
```

Studio uses the unmodified ONNX weights with its independently implemented local detector adapter for editable censor boxes. It does not implement de-censoring, upload images, or run upstream application code. Detection is an aid and may miss regions; box coverage is not pixel segmentation. The model card's license declaration is not a license grant for unified-image-tool code or other models. Source observations were checked on 2026-10-28 (Asia/Tokyo).

The model is a read-only program resource and follows the installed application. Workspaces store only its stable identity and settings; no per-project model copy is created. Explicitly selected custom models continue to use managed local resources.
