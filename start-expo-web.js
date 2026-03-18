const { execSync } = require('child_process');
process.chdir(__dirname + '/Game-Monetizer-main');
require('child_process').spawn('npx', ['expo', 'start', '--web', '--port', '8081'], {
  stdio: 'inherit',
  shell: true,
  env: { ...process.env }
});
