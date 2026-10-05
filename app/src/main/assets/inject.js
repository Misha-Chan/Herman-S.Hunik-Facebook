// Runs with `css` (string) defined by MainActivity.
var d = document;
if (!d.querySelector('meta[name=viewport]')) {
  var m = d.createElement('meta');
  m.name = 'viewport'; m.content = 'width=device-width,initial-scale=1';
  (d.head || d.documentElement).appendChild(m);
}
function apply() {
  if (d.getElementById('__hunik')) return;
  var s = d.createElement('style');
  s.id = '__hunik';
  (d.head || d.documentElement).appendChild(s);
  s.appendChild(d.createTextNode(css));
  var sh = s.sheet;
  if (sh && sh.cssRules && sh.cssRules.length === 0) { // inline style blocked: use CSSOM
    css.split('}').forEach(function (r) {
      r = r.trim();
      if (r) { try { sh.insertRule(r + '}', sh.cssRules.length); } catch (e) {} }
    });
  }
}
apply();
window.__hunikToggle = function () {
  var s = d.getElementById('__hunik');
  if (s) s.disabled = !s.disabled;
};
// Structure only (tag, role, first classes, count). No page text is collected.
window.__hunikDump = function () {
  var all = d.getElementsByTagName('*'), map = {}, order = [];
  for (var i = 0; i < all.length && i < 8000; i++) {
    var e = all[i], role = e.getAttribute('role'), al = e.getAttribute('aria-label');
    var k = e.tagName.toLowerCase() + (role ? '[role=' + role + ']' : '') +
      (al ? '[aria-label=' + al.slice(0, 20) + ']' : '') +
      (typeof e.className === 'string' && e.className ? '.' + e.className.trim().split(/\s+/).slice(0, 3).join('.') : '');
    if (!map[k]) { map[k] = 0; order.push(k); }
    map[k]++;
  }
  var out = location.href.split('?')[0] + '\n' + navigator.userAgent + '\n';
  for (var j = 0; j < order.length && j < 500; j++) out += map[order[j]] + '  ' + order[j] + '\n';
  return out;
};
if (!window.__hunikTimer) window.__hunikTimer = setInterval(apply, 2000);
