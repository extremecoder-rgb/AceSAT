const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Logger middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Initialize NVIDIA NIM client
const apiKey = process.env.NVIDIA_API_KEY;
if (!apiKey || apiKey.includes('YOUR_NVIDIA_API_KEY')) {
  console.error('============================================================');
  console.error('ERROR: NVIDIA_API_KEY is not set!');
  console.error('Get a FREE API key from: https://build.nvidia.com/models');
  console.error('Then set it in backend/.env as: NVIDIA_API_KEY=nvapi-...');
  console.error('============================================================');
}

const client = new OpenAI({
  baseURL: 'https://integrate.api.nvidia.com/v1',
  apiKey: apiKey || 'missing'
});

// OpenAI compatible chat completion endpoint — always calls NVIDIA NIM
app.post('/v1/chat/completions', async (req, res) => {
  if (!apiKey || apiKey.includes('YOUR_NVIDIA_API_KEY')) {
    return res.status(500).json({
      error: 'NVIDIA_API_KEY not configured. Get a free key from https://build.nvidia.com/models and set it in .env'
    });
  }

  const { model, messages, temperature, top_p, max_tokens } = req.body;

  try {
    console.log(`Calling NVIDIA NIM (Model: ${model || 'nemotron-3-super-120b-a12b'})...`);
    console.log(`Messages count: ${messages?.length || 0}`);

    const response = await client.chat.completions.create({
      model: model || 'nvidia/nemotron-3-super-120b-a12b',
      messages: messages,
      temperature: temperature !== undefined ? temperature : 0.8,
      top_p: top_p !== undefined ? top_p : 0.95,
      max_tokens: max_tokens !== undefined ? max_tokens : 8000
    });

    console.log('Successfully received response from NVIDIA NIM.');
    res.json(response);
  } catch (error) {
    console.error('NVIDIA NIM Error:', error.message);
    
    // If rate limited or model error, provide useful feedback
    if (error.status === 429) {
      return res.status(429).json({
        error: 'Rate limited by NVIDIA NIM. Please wait a moment and try again.',
        details: error.message
      });
    }
    
    res.status(error.status || 500).json({
      error: 'Failed to get response from NVIDIA NIM.',
      details: error.message
    });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  const hasKey = apiKey && !apiKey.includes('YOUR_NVIDIA_API_KEY');
  res.json({
    status: hasKey ? 'OK' : 'MISSING_API_KEY',
    mode: hasKey ? 'Live NVIDIA NIM' : 'NOT CONFIGURED',
    message: hasKey
      ? 'AceSAT Backend is connected to NVIDIA NIM.'
      : 'Set NVIDIA_API_KEY in .env to enable AI. Get a free key from https://build.nvidia.com/models'
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n🚀 AceSAT Backend running on http://0.0.0.0:${PORT}`);
  console.log(`   Health check: http://localhost:${PORT}/health`);
  if (apiKey && !apiKey.includes('YOUR_NVIDIA_API_KEY')) {
    console.log('   ✅ NVIDIA NIM API key detected — AI is LIVE');
  } else {
    console.log('   ❌ No API key — set NVIDIA_API_KEY in .env');
  }
  console.log('');
});

// Export the Express app for Vercel Serverless Functions
module.exports = app;
