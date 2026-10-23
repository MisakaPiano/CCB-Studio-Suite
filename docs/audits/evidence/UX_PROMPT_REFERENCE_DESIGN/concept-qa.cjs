const {chromium}=require('C:/Users/1/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('fs');
const assert=require('assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',args:['--disable-background-networking','--disable-component-update']});
 try{
  const page=await browser.newPage({viewport:{width:1056,height:1200},colorScheme:'dark'}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto('file:///H:/CCB-Studio-Suite/.local/prompt-reference-proposal-preview.html');
  const f=page.frameLocator('iframe'),app=f.locator('.p-app[data-layout="bottom"]');
  await app.locator('.p-source-count').filter({hasText:'3 / 16'}).waitFor();
  const close=async()=>app.locator('.p-dialog[open] [data-close]').click();
  const draft=async()=>{await app.locator('[data-do="draft"]').click();const text=await app.locator('.p-dialog pre').innerText();await close();return text;};
  assert.equal(await draft(),'garden, morning light');
  await app.locator('[data-take="ai"][data-key="base"]').click();
  assert.equal(await app.locator('.p-excerpt-text').inputValue(),'two friends, birthday party, garden');
  await app.locator('[data-take="image"][data-key="style"]').click();
  assert.match(await app.locator('.p-excerpt-text').inputValue(),/watercolor, warm light/);
  assert.equal(await draft(),'garden, morning light');
  await app.locator('[data-do="apply"]').click();await app.locator('.p-dialog button').filter({hasText:'取消应用'}).click();
  assert.equal(await draft(),'garden, morning light');
  await app.locator('[data-do="apply"]').click();await app.locator('.p-dialog button').filter({hasText:'确认应用，可撤销'}).click();
  assert.match(await draft(),/two friends/);await app.locator('[data-do="undo"]').click();assert.equal(await draft(),'garden, morning light');
  await app.locator('[data-role-down="image:0"]').click();
  assert.match(await app.locator('[data-drag-role="image:0"]').innerText(),/原始角色 A/);
  await app.locator('[data-drag-role="image:0"]').dragTo(app.locator('[data-role-source="image"][data-role-index="1"]'));
  assert.match(await app.locator('[data-drag-role="image:0"]').innerText(),/原始角色 B/);
  await app.locator('[data-info="image"]').click();assert.match(await app.locator('.p-dialog').innerText(),/原始角色顺序：B → A/);
  await app.locator('.p-dialog button').filter({hasText:'实际请求快照'}).click();assert.match(await app.locator('.p-dialog').innerText(),/没有已验证/);await close();
  await app.locator('.p-diff').check();assert.equal(await app.locator('tbody tr').filter({has:app.getByText('负向 Prompt',{exact:true})}).count(),0);
  await app.locator('[data-panel="params"]').click();assert.match(await app.locator('tbody').innerText(),/未记录/);
  await app.locator('.p-diff').uncheck();await app.locator('[data-panel="prompt"]').click();
  await app.locator('[data-remove="text"]').click();assert.match(await app.locator('.p-source-count').innerText(),/^2/);
  await app.locator('[data-do="sources"]').click();await app.locator('.p-dialog [data-add="text"]').click();assert.match(await app.locator('.p-source-count').innerText(),/^3/);
  assert.equal(await app.locator('.p-dialog [data-add="text"]').isDisabled(),true);
  await app.locator('.p-dialog [data-new-text]').click();await app.locator('.p-dialog textarea').fill('new fixture, mountain');
  await app.locator('.p-dialog button').filter({hasText:'保存并加入'}).click();assert.match(await app.locator('.p-source-count').innerText(),/^4/);
  assert.equal(await draft(),'garden, morning light');
  await app.locator('[data-do="save"]').click();await app.locator('[data-do="sources"]').click();assert.match(await app.locator('.p-dialog').innerText(),/摘取 Prompt 1/);await close();
  await page.screenshot({path:'H:/CCB-Studio-Suite/.local/prompt-reference-proposal-bottom.png',fullPage:true});
  const widths=[];
  for(const layout of ['bottom','right']){
   if(layout==='right')await f.getByRole('button',{name:'下一布局',exact:true}).click();
   for(const width of [1024,736,320]){
    await page.setViewportSize({width,height:1200});await page.waitForTimeout(120);
    const overflow=await f.locator('.p-app[data-layout="'+layout+'"]').evaluate(el=>({w:el.clientWidth,scroll:el.scrollWidth,root:document.documentElement.scrollWidth,view:document.documentElement.clientWidth}));
    assert.ok(overflow.w<=width,JSON.stringify({layout,width,overflow}));assert.ok(overflow.scroll<=overflow.w+1,JSON.stringify({layout,width,overflow}));assert.ok(overflow.root<=overflow.view+1);widths.push({layout,width,...overflow});
   }
  }
  await page.setViewportSize({width:1056,height:1200});await page.screenshot({path:'H:/CCB-Studio-Suite/.local/prompt-reference-proposal-right.png',fullPage:true});
  assert.deepEqual(errors,[]);
  fs.writeFileSync('H:/CCB-Studio-Suite/.local/prompt-reference-proposal-qa.json',JSON.stringify({status:'PASS',scope:'interactive concept only; no production UI or real providers',checks:['three source types without draft overwrite','excerpt across fields','apply cancel and confirm/undo','actual role drag without changing original order','unknown request distinction','difference-only','source removal and deduplicated re-add','standalone text direct entry','excerpt saved into concept source pool','both layouts at three widths'],widths,errors},null,2));
  process.stdout.write('PASS: Prompt concept interactions and responsive layouts\n');
 }finally{await browser.close();}
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exit(1);});
