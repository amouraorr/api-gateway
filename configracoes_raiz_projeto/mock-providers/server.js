const express = require('express');
const app = express();
app.use(express.json());

app.post('/email', (req, res) => {
  console.log('POST /email', req.body);
  // retornar 200 com corpo opcional
  res.status(200).json({ok: true, provider: 'email', received: req.body});
});

app.post('/sms', (req, res) => {
  console.log('POST /sms', req.body);
  res.status(200).json({ok: true, provider: 'sms', received: req.body});
});

// opcional: healthcheck
app.get('/health', (req, res) => res.send('ok'));

const port = 80;
app.listen(port, () => console.log(`mock-providers listening on ${port}`));