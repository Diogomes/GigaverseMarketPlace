// Aplica o tema salvo antes da renderização para evitar flash de tema errado.
// Arquivo externo (não inline) para a Content-Security-Policy da loja permitir só script-src 'self'.
try {
  var t = localStorage.getItem('gv3d-theme')
  if (t === 'light' || t === 'dark') document.documentElement.dataset.theme = t
} catch (e) {}
