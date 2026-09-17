// Run with node. Uses the actual public Antigravity menu implementation in an
// isolated fixture, without logging in, loading a workspace, or calling its API.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '..');
const origin = 'https://antigravity.static.usercontent.goog';
const bundleUrl = origin + '/web/1.0.20260910010023/main.js';
const bridge = fs.readFileSync(path.join(root, 'app/src/main/java/com/hkm/antigravity/ui/components/EffortPickerBridge.kt'), 'utf8').split('"""')[1];

(async () => {
  const response = await fetch(bundleUrl);
  assert(response.ok);
  const bundle = await response.text();
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 412, height: 915 }, hasTouch: true, isMobile: true });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    await context.route('**/*', async route => {
      const url = route.request().url();
      if (url === 'https://wrapper.test/') return route.fulfill({ contentType: 'text/html', body: '<meta name="viewport" content="width=device-width"><iframe style="width:390px;height:850px;border:0" src="'+origin+'/fixture"></iframe>' });
      if (url === origin + '/fixture') return route.fulfill({ contentType: 'text/html', body: '<meta name="viewport" content="width=device-width"><style>body{margin:0}button,[role^=menuitem]{display:block;min-height:48px;min-width:160px;padding:8px}[role=menu]{background:white;border:1px solid black;padding:8px}</style><script>window.process={env:{NODE_ENV:"production"},platform:"linux"};window.__APP_CONFIG__={productName:"antigravity",isRemoteControl:true};</script><script src="/fixture-bundle.js"></script>' });
      if (url === origin + '/fixture-bundle.js') return route.fulfill({ contentType: 'application/javascript', body: bundle });
      return route.abort();
    });
    const mode = process.argv[2] || 'frame';
    await context.addInitScript(({ code, mode, origin }) => {
      if (mode === 'parent' ? window === window.top : location.origin === origin) (0, eval)(code);
    }, { code: bridge, mode, origin });
    await page.goto('https://wrapper.test/');
    const frame = page.frames().find(f => f.url().startsWith(origin));
    await frame.waitForFunction(() => typeof CQ === 'function' && typeof JA === 'function');
    await frame.evaluate(() => {
      window.selected = null;
      const el = document.createElement('div'); document.body.append(el);
      const h = z.createElement;
      function Fixture() {
        const [open, setOpen] = z.useState(false);
        const select = effort => { window.selected = effort; setOpen(false); };
        return h(JA, {open, onOpenChange:setOpen},
          h(pNa, null, 'Models'),
          h(SMa, null, h(HA, {side:'bottom',align:'start'}, h(EA,null,
            h(gNa,null,
              h(CQ, {onClick:()=>select('default')}, h('span',{'data-testid':'model-selector-effort-group','data-model-base':'Test Model'}, 'Test Model')),
              h(SMa,null,h(HA,{side:'right',align:'start'},h(EA,null,
                ['low','medium','high'].map(e=>h(OMa,{key:e,onClick:()=>select(e)},h('span',{'data-testid':'model-selector-effort-option','data-effort':e},e)))
              )))
            ),
            h(OMa, {onClick:()=>select('plain'),'data-testid':'model-selector-item'}, 'Plain model')
          )))
        );
      }
      vt.createRoot(el).render(h(Fixture));
    });
    await frame.getByRole('button', {name:'Models',exact:true}).tap();
    await frame.locator('[data-testid="model-selector-effort-group"]').tap();
    await page.waitForTimeout(300);
    const before = await frame.evaluate(() => window.selected);
    if (mode === 'parent') {
      assert.equal(before, 'default', 'baseline must reproduce premature selection');
      console.log('PASS: parent-only injection reproduces immediate model selection and closure');
    } else {
      assert.equal(before, null, 'touch on effort group must not select default model');
      await frame.locator('[data-effort="high"]').tap();
      assert.equal(await frame.evaluate(() => window.selected), 'high');
      console.log('PASS: touch opens submenu and selects High with actual upstream menu components');
      await frame.getByRole('button',{name:'Models',exact:true}).tap();
      await frame.locator('[data-testid="model-selector-item"]').tap();
      assert.equal(await frame.evaluate(() => window.selected), 'plain');
      console.log('PASS: model without effort still selects normally');
      for (const effort of ['low', 'medium', 'high', 'low']) {
        await frame.getByRole('button',{name:'Models',exact:true}).tap();
        const previous = await frame.evaluate(() => window.selected);
        // Tap row whitespace as well as the label.
        await frame.getByRole('menuitem', {name:'Test Model',exact:true}).tap({position:{x:4,y:4}});
        assert.equal(await frame.evaluate(() => window.selected), previous);
        await frame.locator('[data-effort="'+effort+'"]').tap();
        assert.equal(await frame.evaluate(() => window.selected), effort);
        await frame.locator('[data-testid="model-selector-effort-group"]').waitFor({state:'hidden'});
      }
      console.log('PASS: repeated Low/Medium/High selection and row whitespace taps');
      await frame.getByRole('button',{name:'Models',exact:true}).click();
      await frame.locator('[data-testid="model-selector-effort-group"]').click();
      assert.equal(await frame.evaluate(() => window.selected), 'default');
      console.log('PASS: desktop mouse click keeps upstream default-selection behavior');
      await frame.getByRole('button',{name:'Models',exact:true}).click();
      await frame.getByRole('menuitem', {name:'Test Model',exact:true}).focus();
      await page.keyboard.press('ArrowRight');
      await frame.locator('[data-effort="medium"]').waitFor({state:'visible'});
      await frame.waitForFunction(() => document.activeElement?.textContent === 'low');
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Enter');
      assert.equal(await frame.evaluate(() => window.selected), 'medium');
      console.log('PASS: keyboard navigation and Enter selection unchanged');
      await frame.getByRole('button',{name:'Models',exact:true}).click();
      await frame.locator('[data-testid="model-selector-effort-group"]').hover();
      await frame.locator('[data-effort="high"]').click();
      assert.equal(await frame.evaluate(() => window.selected), 'high');
      console.log('PASS: desktop hover submenu and effort click unchanged');
    }
    assert.deepEqual(errors, [], 'no document-start or runtime JavaScript errors');
    await context.close();
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
