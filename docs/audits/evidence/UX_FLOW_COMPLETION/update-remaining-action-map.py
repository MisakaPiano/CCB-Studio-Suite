from pathlib import Path
import json
r=Path.cwd();p=r/'docs/ux/ACTION_IMPLEMENTATION.json';data=json.loads(p.read_text(encoding='utf-8'))
def source(name):
    hits=list((r/'modules').glob('*/src/main/**/'+name))
    assert len(hits)==1,(name,hits)
    return hits[0].relative_to(r).as_posix()
rows={
 'P15':(['GalleryWorkspace.kt','GalleryAlbums.kt'],['GalleryAlbumWorkflowTest.sourceAlbumsNavigateSelectFilterAndPersistDepthPreferencesWithoutChangingRecipes'],'UX_HISTORY_REFERENCE'),
 'I05':(['TypesetExportDialog.kt','TypesetExport.kt'],['TypesetMetadataExportTest','TypesetWorkflowTest.fourCategoryPreviewCopiesCompleteFileDeckWithoutSystemClipboard'],'UX_FLOW_COMPLETION'),
 'I08':(['TypesetExport.kt','SharedAssets.kt'],['TypesetMetadataExportTest','SharedAssetsTest'],'UX_FLOW_COMPLETION'),
 'Q03':(['TypesetFullPreview.kt'],['TypesetWorkflowTest.fullPreviewKeyboardAndExportDeckKeepEditorStateAndFitNarrowWindow'],'UX_FLOW_COMPLETION'),
 'Q04':(['TypesetSourceExchange.kt','TypesetEditorController.kt'],['TypesetSourceExchangeTest','TypesetWorkflowTest.sourceExchangePreviewConfirmExportAndReopen'],'UX_FLOW_COMPLETION'),
 'C02':(['AssetStore.java','AssetBackup.java','StudioWorkspace.kt'],['AssetStoreTest','AssetSafetyTest','AssetMaintenanceTest','AssetBackupTest','StudioMaintenanceTest.maintenanceRejectsNewEditsAndKeepsRootOwnedUntilWriterDrains'],'UX_STARTUP_SAFETY'),
 'C04':(['StudioFormatMigration.kt','StudioWorkspace.kt','StudioDataLaunch.kt'],['StudioVersionTwoTest','StudioFormatMigrationTest','StudioDataLaunchTest'],'UX_STARTUP_SAFETY'),
 'C05':(['SecretVault.kt','LiveRequestBudget.kt','StudioApplication.kt'],['LiveRequestBudgetTest','StudioMaintenanceTest.snapshotPreservesOriginalAssociationsAndDpapiCredentialWithoutPlaintext','WorkbenchWorkflowTest.accountsDesignApplyGenerateGalleryAndRestart'],'UX_UPDATE_FEEDBACK'),
 'C07':(['StudioBrand.kt','StudioTitleControls.kt','StudioApplication.kt'],['StudioTitleControlsTest.captionGeometryAndCommandsAtFourDisplayScales','WorkbenchWorkflowTest.measuredSuiteNavigationSurvivesResizeAndUnsentInput'],'UX_UPDATE_FEEDBACK'),
}
story=['TypesetWorkflowTest.storyCaptionPresetPreviewExportAndRestart','TypesetWorkflowTest.storyCustomControlsSelectedAndAllStylesPreserveCaptionsAndUndoAsOneBatch','TypesetReferenceTest.sourceStoryGeometryAndBlankParagraphs']
for i in range(1,10):rows[f'F0{i}']=(['TypesetWorkspace.kt','TypesetBook.kt','TypesetRenderer.kt','TypesetExport.kt'],story+(['TypesetExportTest'] if i>=8 else []),'UX_FLOW_COMPLETION')
for a in data['actions']:
    if a['id'] not in rows:continue
    files,tests,evidence=rows[a['id']]
    a.update(implementation=[source(n) for n in files],tests=tests,
        guiEvidence=[f'docs/audits/{evidence}.md (scope and test kind disclosed; not human acceptance)'],
        persistenceEvidence=['Actual test assertions and restart/byte-preservation evidence in cited report; no automatic promotion from source presence'],
        status='ENGINEERING_PARTIAL / EXECUTED_REGRESSION / PRODUCT_HOLD',
        remainingVerification=['Fixed installed package interaction and concentrated human acceptance remain separate'])
    if a['id']=='C07':a['remainingVerification']=['Native IME/focus/multiwindow/high-DPI visual acceptance NOT TESTED; Compose density test is not native display certification']
    if a['id']=='F07':a['remainingVerification']+=['Ctrl+Enter handler exists; current new regression uses next-caption button, not native IME/shortcut interaction']
    if a['id']=='I05':a['remainingVerification']+=['OS clipboard receiving application paste is separate from injected file-deck test']
p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Backfilled',len(rows),'actions without changing product acceptance')


