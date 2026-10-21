/* Isolated read-only reference run. No product WebView or external requests. */
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'.local/typeset/colors',new Date().toISOString().replace(/[:.]/g,'-'));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
fs.mkdirSync(out,{recursive:true});let browser;
(async()=>{
 const raw=fs.readFileSync(path.join(root,'.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html'));
 if(sha(raw)!=='892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63')throw Error('Source identity');
 let html=raw.toString('utf8');const a=html.indexOf('const DEFAULT_PREFS = {'),b=html.indexOf('\n};',a)+3,defaults=html.slice(a,b);
 if(a<0||b<a)throw Error('Bootstrap precondition');
 html=html.slice(0,a)+html.slice(b);html=html.replace('function defaultStorybookConfig(){',defaults+'\nfunction defaultStorybookConfig(){');
 html=html.replace('</script>','window.__colorReference={parseRichTextTokens,defaultText};</script>');
 const log={sourceSha256:sha(raw),instrumentedSha256:sha(html),instrumentation:'Known initialization-order correction and test hook only; original rich-text parser unchanged',cases:[],blocked:[],human:'NOT TESTED'};
 const names=JSON.parse(fs.readFileSync(path.join(root,'modules/studio-core/src/main/resources/typeset/css-named-colors.json'),'utf8'));
 const colors=[...Object.keys(names),'transparent','#123','#1234','#11223344','rgb(17, 34, 51)','rgba(17,34,51,0.4)','rgb(10% 20% 30% / 50%)','hsl(210 50% 40% / 25%)','hsla(-150deg,50%,40%,0.25)','hsl(0.5turn 100% 50%)','hwb(210 10% 20% / 50%)','rgb(300 -20 51)','rgb(none 20 30 / none)','hsl(1.5707963267948966rad 100% 50%)'];
 browser=await chromium.launch({channel:'msedge',headless:true,args:['--disable-gpu','--disable-extensions','--no-first-run']});log.browser=browser.version();
 const page=await browser.newPage();await page.route('**/*',r=>r.request().url()==='https://typeset.invalid/'?r.fulfill({contentType:'text/html',body:html}):(log.blocked.push(r.request().url()),r.abort()));await page.goto('https://typeset.invalid/');
 await page.waitForFunction(()=>!!window.__colorReference);
 log.cases=await page.evaluate(colors=>colors.map(value=>{
   const r=window.__colorReference,c=document.createElement('canvas');c.width=2;c.height=1;const g=c.getContext('2d');
   const base={fontFamily:'sans-serif',fontSize:24,fontWeight:400,fontStyle:'normal',underline:false,strike:false,fill:'#112233',stroke:'#ffffff',strokeWidth:0};
   const tokens=r.parseRichTextTokens(`[color="${value}"]X[/]`,base);
   const token=tokens.find(t=>t.ch==='X');if(!token)throw Error('Missing original parser token');
   const parsed=token.style.fill;g.fillStyle='#fff';g.fillRect(0,0,1,1);g.fillStyle=parsed;g.fillRect(0,0,2,1);
   const pixel=Array.from(g.getImageData(0,0,2,1).data);
   return {value,parsed,overWhite:pixel.slice(0,4),rgba:pixel.slice(4,8),supported:CSS.supports('color',value)};
 }),colors);
 for(const item of log.cases)if(!item.supported||item.parsed!==item.value)throw Error('Unexpected reference parsing: '+item.value);
 log.namedValuesChecked=Object.entries(names).every(([name,hex])=>{const got=log.cases.find(c=>c.value===name).rgba;return hex.toLowerCase()==='#'+got.slice(0,3).map(n=>n.toString(16).padStart(2,'0')).join('')&&got[3]===255});
 if(!log.namedValuesChecked)throw Error('Named-color data differs from browser');
 fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify(log,null,2));console.log(out);await browser.close();
})().catch(async e=>{fs.writeFileSync(path.join(out,'failure.txt'),String(e.stack||e));console.error(e);if(browser)await browser.close();process.exitCode=1;});
