const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Logger middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// A library of mock questions for demo mode
const mockQuestions = {
  "Math": {
    "Linear Equations": {
      "Easy": {
        "question": "Solve for x: 2x + 5 = 15.",
        "options": { "A": "3", "B": "5", "C": "7", "D": "10" },
        "correctAnswer": "B",
        "explanation": "Subtract 5 from both sides: 2x = 10. Divide by 2: x = 5."
      },
      "Medium": {
        "question": "A car rental company charges $20 per day plus $0.15 per mile. If John rented a car for 3 days and his total bill was $82.50, how many miles did he drive?",
        "options": { "A": "150", "B": "200", "C": "100", "D": "250" },
        "correctAnswer": "A",
        "explanation": "The base charge is 3 * $20 = $60. The remaining charge is $82.50 - $60 = $22.50. Dividing $22.50 by $0.15 gives 150 miles."
      },
      "Hard": {
        "question": "If the system of linear equations 3x - 5y = 8 and ax + 10y = -16 has infinitely many solutions, what is the value of a?",
        "options": { "A": "6", "B": "-6", "C": "3", "D": "-3" },
        "correctAnswer": "B",
        "explanation": "For a system of equations to have infinitely many solutions, one equation must be a multiple of the other. Multiplying the first equation by -2 gives: -6x + 10y = -16. Thus, a must equal -6."
      }
    },
    "Quadratic Equations": {
      "Easy": {
        "question": "What are the roots of the equation x^2 - 9 = 0?",
        "options": { "A": "x = 3", "B": "x = -3", "C": "x = 3 and x = -3", "D": "x = 0" },
        "correctAnswer": "C",
        "explanation": "Factoring x^2 - 9 gives (x - 3)(x + 3) = 0. Therefore, the roots are x = 3 and x = -3."
      },
      "Medium": {
        "question": "What is the sum of the solutions to the equation x^2 - 7x + 12 = 0?",
        "options": { "A": "7", "B": "-7", "C": "12", "D": "5" },
        "correctAnswer": "A",
        "explanation": "By Vieta's formulas, the sum of the solutions to a quadratic equation ax^2 + bx + c = 0 is given by -b/a. Here, a = 1 and b = -7, so the sum is -(-7)/1 = 7."
      },
      "Hard": {
        "question": "For what value of k will the quadratic equation 4x^2 + kx + 9 = 0 have exactly one real solution?",
        "options": { "A": "6", "B": "12", "C": "12 or -12", "D": "36 or -36" },
        "correctAnswer": "C",
        "explanation": "For a quadratic equation to have exactly one real solution, its discriminant must be zero. b^2 - 4ac = 0 => k^2 - 4(4)(9) = 0 => k^2 - 144 = 0 => k = 12 or -12."
      }
    }
  },
  "Reading & Writing": {
    "Inference": {
      "Easy": {
        "question": "Recent excavations reveal that ancient Mesopotamian cities were far more sprawling than previously assumed, containing large open agricultural fields within city walls. What can be inferred about the city structure?",
        "options": { 
          "A": "Mesopotamian cities were entirely rural.",
          "B": "Food production occurred to some extent inside urban limits.",
          "C": "No trading happened outside city walls.",
          "D": "City walls were built primarily to protect crops."
        },
        "correctAnswer": "B",
        "explanation": "Having agricultural fields inside city walls implies that crop growth and food production occurred inside urban limits."
      },
      "Medium": {
        "question": "In a study of bird migration, researchers noted that younger birds migrating for the first time often strayed hundreds of miles off course during cloudy nights, whereas adult birds maintained their trajectory regardless of cloud cover. What does this suggest?",
        "options": {
          "A": "Adult birds fly faster than younger birds.",
          "B": "Younger birds depend on celestial navigation cues that clouds block, whereas adults have developed additional navigation mechanisms.",
          "C": "Clouds present a physical barrier that younger birds cannot fly over.",
          "D": "Migration routes change every year for older birds."
        },
        "correctAnswer": "B",
        "explanation": "Since cloud cover impacts only younger birds, it suggests they rely on cues blocked by clouds (like stars), while adults do not, implying adults have alternative navigation methods."
      },
      "Hard": {
        "question": "While searching for water on Mars, researchers discovered clay mineral deposits rich in iron and magnesium. On Earth, these minerals form only in neutral-to-alkaline water. What can be inferred about Martian geological history?",
        "options": {
          "A": "Mars never had water on its surface.",
          "B": "Martian water was always highly acidic.",
          "C": "Martian water was at some point neutral or alkaline.",
          "D": "Clay minerals on Mars formed in the absence of liquid water."
        },
        "correctAnswer": "C",
        "explanation": "Since these clay minerals require neutral-to-alkaline water to form on Earth, their presence on Mars implies that Martian water must have had a neutral-to-alkaline pH at some point in the past."
      }
    },
    "Grammar & Punctuation": {
      "Easy": {
        "question": "Choose the option that correctly completes the sentence: The team of scientists ___ conducting groundbreaking research in Antarctica.",
        "options": { "A": "are", "B": "is", "C": "were", "D": "have been" },
        "correctAnswer": "B",
        "explanation": "The subject of the sentence is 'The team' which is singular. Therefore, the singular verb 'is' is correct."
      },
      "Medium": {
        "question": "Which of the following punctuations correctly completes the sentence: The novel was critically acclaimed ___ however, its commercial sales were disappointing.",
        "options": {
          "A": "; ",
          "B": ", ",
          "C": "; however, ",
          "D": ", however "
        },
        "correctAnswer": "C",
        "explanation": "When 'however' is used to connect two independent clauses, it should be preceded by a semicolon and followed by a comma."
      },
      "Hard": {
        "question": "Which sentence contains correct modifier placement?",
        "options": {
          "A": "Walking down the street, the sunset looked beautiful.",
          "B": "Walking down the street, she thought the sunset looked beautiful.",
          "C": "She thought the sunset looked beautiful walking down the street.",
          "D": "Beautiful, walking down the street, the sunset was seen by her."
        },
        "correctAnswer": "B",
        "explanation": "Option B has the correct modifier placement. The introductory phrase 'Walking down the street' must modify the subject 'she', who is performing the action, not 'the sunset'."
      }
    }
  }
};

// OpenAI compatible chat completion endpoint
app.post('/v1/chat/completions', async (req, res) => {
  const apiKey = process.env.NVIDIA_API_KEY;
  const { model, messages, temperature, top_p, max_tokens } = req.body;

  // 1. OFFLINE / MOCK DEMO MODE FALLBACK
  if (!apiKey || apiKey.includes('YOUR_NVIDIA_API_KEY')) {
    console.log('--- OFFLINE DEMO MODE: Simulating NVIDIA NIM Response ---');
    const userMessage = messages[messages.length - 1].content;
    const systemMessage = messages.find(m => m.role === 'system')?.content || '';

    let contentStr = '';

    // Handle Study Plan Generation Request
    if (systemMessage.includes('study plan') || userMessage.toLowerCase().includes('study plan')) {
      console.log('Generating Mock Study Plan...');
      const studyPlan = [
        {
          "stepOrder": 1,
          "title": "Master Quadratic Factoring",
          "description": "Review factoring quadratics, quadratic formula, and practicing finding roots.",
          "category": "Quadratic Equations",
          "estimatedMinutes": 30
        },
        {
          "stepOrder": 2,
          "title": "Linear Word Problems Practice",
          "description": "Practice setting up linear systems from word problems and solving for variables.",
          "category": "Linear Equations",
          "estimatedMinutes": 45
        },
        {
          "stepOrder": 3,
          "title": "Inference Text Breakdown",
          "description": "Identify evidence-based conclusions in SAT Reading passages and reject extreme claims.",
          "category": "Inference",
          "estimatedMinutes": 30
        },
        {
          "stepOrder": 4,
          "title": "Punctuation Rules Drill",
          "description": "Practice semicolons, colons, dashes, and comma splices in Writing questions.",
          "category": "Grammar & Punctuation",
          "estimatedMinutes": 30
        }
      ];
      contentStr = JSON.stringify(studyPlan);
    } 
    // Handle Question Generation Request
    else {
      console.log('Generating Mock SAT Question...');
      // Extract target category and difficulty from prompts
      let difficulty = "Medium";
      if (userMessage.includes("Easy") || systemMessage.includes("Easy")) difficulty = "Easy";
      if (userMessage.includes("Hard") || systemMessage.includes("Hard")) difficulty = "Hard";

      let category = "Linear Equations";
      let section = "Math";

      if (userMessage.includes("Quadratic") || systemMessage.includes("Quadratic")) {
        category = "Quadratic Equations";
        section = "Math";
      } else if (userMessage.includes("Inference") || systemMessage.includes("Inference")) {
        category = "Inference";
        section = "Reading & Writing";
      } else if (userMessage.includes("Grammar") || systemMessage.includes("Grammar") || userMessage.includes("Punctuation") || systemMessage.includes("Punctuation")) {
        category = "Grammar & Punctuation";
        section = "Reading & Writing";
      }

      const q = mockQuestions[section]?.[category]?.[difficulty] || mockQuestions["Math"]["Linear"]["Medium"];
      contentStr = JSON.stringify(q);
    }

    // Return mocked response structure matching OpenAI / NVIDIA completions API
    return res.json({
      id: 'chatcmpl-mock',
      object: 'chat.completion',
      created: Math.floor(Date.now() / 1000),
      model: model || 'nvidia/nemotron-3-super-120b-a12b',
      choices: [
        {
          index: 0,
          message: {
            role: 'assistant',
            content: contentStr
          },
          finish_reason: 'stop'
        }
      ]
    });
  }

  // 2. LIVE MODE: CALL NVIDIA NIM ENDPOINT
  try {
    const client = new OpenAI({
      baseURL: 'https://integrate.api.nvidia.com/v1',
      apiKey: apiKey
    });

    console.log(`Forwarding chat completion to NVIDIA NIM (Model: ${model})...`);

    const response = await client.chat.completions.create({
      model: model || 'nvidia/nemotron-3-super-120b-a12b',
      messages: messages,
      temperature: temperature !== undefined ? temperature : 1.0,
      top_p: top_p !== undefined ? top_p : 0.95,
      max_tokens: max_tokens !== undefined ? max_tokens : 4000,
      extra_body: {
        chat_template_kwargs: { enable_thinking: true },
        reasoning_budget: 16384
      }
    });

    console.log('Successfully received response from NVIDIA NIM.');
    res.json(response);
  } catch (error) {
    console.error('Error occurred in NVIDIA NIM completion:', error.message);
    res.status(500).json({
      error: 'Proxy server encountered an error talking to NVIDIA API.',
      details: error.message
    });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    mode: (!process.env.NVIDIA_API_KEY || process.env.NVIDIA_API_KEY.includes('YOUR_NVIDIA_API_KEY')) ? 'Offline Demo Mode' : 'Live NVIDIA NIM Mode',
    message: 'AceSAT Backend Proxy is healthy and running.'
  });
});

app.listen(PORT, () => {
  console.log(`AceSAT Backend Proxy running on port ${PORT}`);
  console.log(`Listening for OpenAI compatible POST requests at /v1/chat/completions`);
});
