const http = require('http');
const fs = require('fs');
const path = require('path');

http.createServer((req, res) => {
  const fp = path.join(__dirname, 'web', req.url === '/' ? 'index.html' : req.url);
  fs.readFile(fp, (err, data) => {
    if (err) { res.writeHead(404); res.end('Not found'); return; }
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(data);
  });
}).listen(3000, () => console.log('Server running on http://localhost:3000'));
