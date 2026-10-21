// Isolated pinned source exporter/importer. Original HTML and algorithm are never modified on disk.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),{chromium}=require('playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'.local/typeset/exchange',new Date().toISOString().replace(/[:.]/g,'-'));
fs.mkdirSync(out,{recursive:true});const hash=b=>crypto.createHash('sha256').update(b).digest('hex');let browser;
const report={status:'RUNNING',errors:[],blocked:[],instrumentation:['Move DEFAULT_PREFS after STORYBOOK_PRESETS to bypass previously recorded source TDZ startup failure','Expose original exporter/importer and fixture state; exchange algorithms unchanged'],humanReview:'NOT TESTED'};
(async()=>{
 const raw=fs.readFileSync(path.join(root,'.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html'));report.sourceSha256=hash(raw);
 if(report.sourceSha256!=='892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63')throw Error('Source identity');
 let html=raw.toString('utf8');const start=html.indexOf('const DEFAULT_PREFS = {'),end=html.indexOf('\n};',start)+3,defaults=html.slice(start,end);
 html=(html.slice(0,start)+html.slice(end)).replace('function defaultStorybookConfig(){',defaults+'\nfunction defaultStorybookConfig(){');
 html=html.replace('</script>','window.__exchange={state,defaultText,loadImageFiles,loadEntry,exportCurrentWorkspaceFile,importWorkspaceFile,STORYBOOK_PRESETS,savePrefs,exportPresetsFile};\n</script>');report.instrumentedSha256=hash(Buffer.from(html));
 browser=await chromium.launch({channel:'msedge',headless:true,args:['--disable-gpu','--disable-extensions','--no-first-run']});report.browser=browser.version();
 const context=await browser.newContext({viewport:{width:1400,height:950},acceptDownloads:true});const page=await context.newPage();page.on('pageerror',e=>report.errors.push(e.message));
 await page.route('**/*',route=>route.request().url()==='https://typeset.invalid/'?route.fulfill({contentType:'text/html',body:html}):(report.blocked.push(route.request().url()),route.abort()));
 await page.goto('https://typeset.invalid/');await page.waitForFunction(()=>!!window.__exchange?.state.workspaceId);
 if(process.argv[2]){
  const input=fs.readFileSync(path.resolve(process.argv[2]),'utf8');report.suiteInputSha256=hash(Buffer.from(input));
  fs.writeFileSync(path.join(out,'suite-input.json'),input);
  await page.evaluate(async text=>{await window.__exchange.importWorkspaceFile(new File([text],'suite.typesetter-workspace.json',{type:'application/json'}));},input);
  report.imported=await page.evaluate(()=>{const s=window.__exchange.state;return{name:s.workspaceName,mode:s.workspaceMode,entries:s.entries.map(e=>({name:e.name,texts:e.texts,masks:e.masks,caption:e.storybook.caption}))}});
  const expected=JSON.parse(input);if(report.imported.entries.length!==expected.entries.length)throw Error('Imported page count');
  report.imported.entries.forEach((entry,i)=>{const e=expected.entries[i];if(JSON.stringify(entry.texts)!==JSON.stringify(e.texts)||JSON.stringify(entry.masks)!==JSON.stringify(e.masks)||entry.caption!==e.storybook.caption)throw Error('Imported editable fields differ');});
 }else{
  await page.evaluate(async()=>{const r=window.__exchange,c=document.createElement('canvas');c.width=180;c.height=120;const g=c.getContext('2d');g.fillStyle='#557799';g.fillRect(0,0,180,120);g.fillStyle='#eeddaa';g.fillRect(30,20,90,70);
   const blob=await new Promise(resolve=>c.toBlob(resolve));await r.loadImageFiles([new File([blob],'source-synthetic.png',{type:'image/png'})]);
   r.state.workspaceName='来源交换合成样本';const e=r.state.entries[0];e.texts=[{...r.defaultText(),text:'[b]源文字[/]「竖排」',x:90,y:10,width:100,fontFamily:'Microsoft YaHei',fontSize:24,writingMode:'vertical'}];
   e.masks=[{shape:'rect',type:'black',x:3,y:4,w:12,h:15,strength:16},{shape:'brush',type:'mosaic',points:[{x:10,y:60},{x:50,y:80}],size:12,strength:8,density:6,stripeThickness:3,pointDensity:8},{shape:'lasso',type:'blur',points:[{x:80,y:80},{x:100,y:80},{x:90,y:100}],strength:4}];
   e.storybook={...r.STORYBOOK_PRESETS.clean,fontFamily:'Microsoft YaHei',caption:'源旁白，保留。'};r.loadEntry(0);
  });
 }
 const download=page.waitForEvent('download');await page.evaluate(()=>window.__exchange.exportCurrentWorkspaceFile());await(await download).saveAs(path.join(out,'workspace.json'));
 await page.evaluate(()=>{const r=window.__exchange;r.savePrefs({textPresets:[{id:'source-style',name:'源预设',style:{...r.defaultText(),fontFamily:'Microsoft YaHei',fontSize:48,writingMode:'vertical'}}],selectedTextPresetId:'source-style',preferredPreset:'heavy_mosaic'});});
 const presetDownload=page.waitForEvent('download');await page.evaluate(()=>window.__exchange.exportPresetsFile());await(await presetDownload).saveAs(path.join(out,'presets.json'));
 const presetBytes=fs.readFileSync(path.join(out,'presets.json'));report.presets={size:presetBytes.length,sha256:hash(presetBytes)};
 await page.screenshot({path:path.join(out,'source-ui.png')});const bytes=fs.readFileSync(path.join(out,'workspace.json'));report.output={size:bytes.length,sha256:hash(bytes)};report.status='PASS';
})().catch(e=>{report.status='FAILED';report.errors.push(e.stack);process.exitCode=1}).finally(async()=>{if(browser)await browser.close();fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify(report,null,2));console.log(out);console.log(JSON.stringify(report,null,2));});
