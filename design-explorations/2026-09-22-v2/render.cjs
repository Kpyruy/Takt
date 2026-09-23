const {chromium}=require('/home/kpyr/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path=require('node:path');
(async()=>{
 const browser=await chromium.launch({headless:true});
 const page=await browser.newPage({viewport:{width:1320,height:1200},deviceScaleFactor:1.5});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const base='http://127.0.0.1:8766/2026-09-22-v2/';
 await page.goto(base);await page.evaluate(()=>document.fonts.ready);
 await page.getByRole('button',{name:'Обрати Г1',exact:true}).click();
 await page.getByRole('button',{name:'Обрати К1',exact:true}).click();
 if(await page.locator('#selection').textContent()!=='Г1 · К1')throw Error('Selection failed');
 await page.reload();
 if(await page.locator('#selection').textContent()!=='Г1 · К1')throw Error('Persistence failed');
 await page.evaluate(()=>localStorage.removeItem('takt-ui-v2-choice'));
 for(const name of (process.argv.length>2?process.argv.slice(2):['home','calendar','subjects','course','exam','progress'])){
  await page.goto(base+'?export&board='+name);await page.evaluate(()=>document.fonts.ready);
  const bounds=await page.locator('#'+name).boundingBox();
  const overflow=await page.locator('#'+name+' .screen').evaluateAll(els=>els.map(el=>({width:el.clientWidth,scroll:el.scrollWidth,height:el.clientHeight,content:el.scrollHeight})));
  console.log(name,JSON.stringify({bounds,overflow}));
  await page.locator('#'+name).screenshot({path:path.join(__dirname,'previews',name+'.png')});
 }
 await page.setViewportSize({width:390,height:844});await page.goto(base);await page.evaluate(()=>document.fonts.ready);
 console.log('mobile',await page.evaluate(()=>({viewport:innerWidth,content:document.documentElement.scrollWidth})));
 console.log('errors',errors);
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
