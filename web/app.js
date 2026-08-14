// State management
let state = {
  student: null, // { name, targetScore, diagnosticScore, mathScore, rwScore, totalScore }
  weakAreas: [], // [ { category, section, proficiencyScore } ]
  studyPlan: [], // [ { stepOrder, title, description, category, estimatedMinutes, isCompleted } ]
  attempts: [], // [ { category, difficulty, isCorrect } ]
  currentDiagnosticIdx: 0,
  diagnosticAnswers: [],
  currentPracticeQuestion: null,
  selectedPracticeOption: null,
  isPracticeSubmitted: false
};

const PROXY_URL = 'http://localhost:3000/v1/chat/completions';

// DOM Elements
const screens = {
  onboarding: document.getElementById('screen-onboarding'),
  diagnostic: document.getElementById('screen-diagnostic'),
  dashboard: document.getElementById('screen-dashboard'),
  practice: document.getElementById('screen-practice')
};

// Diagnostic Quiz Questions
const diagnosticQuestions = [
  {
    section: "Math",
    category: "Linear Equations",
    text: "If 3x - 4 = 11, what is the value of 2x + 5?",
    options: { "A": "10", "B": "15", "C": "12", "D": "13" },
    correctAnswer: "B"
  },
  {
    section: "Math",
    category: "Quadratic Equations",
    text: "For which of the following values of x is the equation x^2 - 5x + 6 = 0 true?",
    options: { "A": "1", "B": "4", "C": "3", "D": "5" },
    correctAnswer: "C"
  },
  {
    section: "Reading & Writing",
    category: "Inference",
    text: "A study shows that students who read fiction score higher on vocabulary tests. What is the most logical inference from this finding?",
    options: {
      "A": "Reading fiction directly increases vocabulary size.",
      "B": "There is a positive correlation between reading fiction and vocabulary strength.",
      "C": "Fiction readers are generally better at math.",
      "D": "Non-fiction has no impact on vocabulary development."
    },
    correctAnswer: "B"
  }
];

// Initialize App
window.addEventListener('DOMContentLoaded', () => {
  loadFromStorage();
  if (state.student) {
    showScreen('dashboard');
    renderDashboard();
  } else {
    showScreen('onboarding');
  }
  setupEventListeners();
});

function showScreen(screenId) {
  Object.keys(screens).forEach(key => {
    if (key === screenId) {
      screens[key].classList.add('active');
    } else {
      screens[key].classList.remove('active');
    }
  });
}

function loadFromStorage() {
  const student = localStorage.getItem('acesat_student');
  const weakAreas = localStorage.getItem('acesat_weak_areas');
  const studyPlan = localStorage.getItem('acesat_study_plan');
  const attempts = localStorage.getItem('acesat_attempts');

  if (student) state.student = JSON.parse(student);
  if (weakAreas) state.weakAreas = JSON.parse(weakAreas);
  if (studyPlan) state.studyPlan = JSON.parse(studyPlan);
  if (attempts) state.attempts = JSON.parse(attempts);
}

function saveToStorage() {
  localStorage.setItem('acesat_student', JSON.stringify(state.student));
  localStorage.setItem('acesat_weak_areas', JSON.stringify(state.weakAreas));
  localStorage.setItem('acesat_study_plan', JSON.stringify(state.studyPlan));
  localStorage.setItem('acesat_attempts', JSON.stringify(state.attempts));
}

function setupEventListeners() {
  // Onboarding
  document.getElementById('btn-start-diagnostic').addEventListener('click', () => {
    const name = document.getElementById('student-name').value.trim();
    const targetScore = parseInt(document.getElementById('target-score').value) || 1400;

    if (!name) {
      alert('Please enter your name.');
      return;
    }

    state.student = {
      name,
      targetScore,
      diagnosticScore: 0,
      mathScore: 200,
      rwScore: 200,
      totalScore: 400
    };
    saveToStorage();
    
    state.currentDiagnosticIdx = 0;
    state.diagnosticAnswers = [];
    showScreen('diagnostic');
    loadDiagnosticQuestion();
  });

  // Diagnostic Quiz Next
  document.getElementById('btn-diagnostic-next').addEventListener('click', () => {
    const selectedBtn = document.querySelector('.option-item.selected');
    if (!selectedBtn) return;

    const answer = selectedBtn.dataset.option;
    const currentQ = diagnosticQuestions[state.currentDiagnosticIdx];
    const isCorrect = answer === currentQ.correctAnswer;
    state.diagnosticAnswers.push(isCorrect);

    if (state.currentDiagnosticIdx < diagnosticQuestions.length - 1) {
      state.currentDiagnosticIdx++;
      loadDiagnosticQuestion();
    } else {
      finishDiagnosis();
    }
  });

  // Start Practice
  document.getElementById('btn-goto-practice').addEventListener('click', () => {
    showScreen('practice');
    loadPracticeQuestion();
  });

  // Practice Back
  document.getElementById('btn-practice-back').addEventListener('click', () => {
    showScreen('dashboard');
    renderDashboard();
  });

  // Practice Submit
  document.getElementById('btn-practice-submit').addEventListener('click', () => {
    if (state.isPracticeSubmitted || !state.selectedPracticeOption) return;
    submitPracticeAnswer();
  });

  // Practice Next
  document.getElementById('btn-practice-next').addEventListener('click', () => {
    loadPracticeQuestion();
  });
}

// 1. Diagnostic Flow
function loadDiagnosticQuestion() {
  const currentQ = diagnosticQuestions[state.currentDiagnosticIdx];
  
  document.getElementById('diagnostic-progress').textContent = `Diagnostic Quiz (${state.currentDiagnosticIdx + 1}/${diagnosticQuestions.length})`;
  document.getElementById('diagnostic-section').textContent = currentQ.section.toUpperCase();
  document.getElementById('diagnostic-question-text').textContent = currentQ.text;

  const optionsContainer = document.getElementById('diagnostic-options');
  optionsContainer.innerHTML = '';

  Object.entries(currentQ.options).forEach(([key, value]) => {
    const optionBtn = document.createElement('div');
    optionBtn.className = 'option-item';
    optionBtn.textContent = `${key}. ${value}`;
    optionBtn.dataset.option = key;

    optionBtn.addEventListener('click', () => {
      document.querySelectorAll('#diagnostic-options .option-item').forEach(b => b.classList.remove('selected'));
      optionBtn.classList.add('selected');
      
      const nextBtn = document.getElementById('btn-diagnostic-next');
      nextBtn.classList.remove('disabled');
      nextBtn.removeAttribute('disabled');
    });

    optionsContainer.appendChild(optionBtn);
  });

  const nextBtn = document.getElementById('btn-diagnostic-next');
  nextBtn.classList.add('disabled');
  nextBtn.setAttribute('disabled', 'true');
  nextBtn.textContent = state.currentDiagnosticIdx === diagnosticQuestions.length - 1 ? 'FINISH ASSESSMENT' : 'NEXT QUESTION';
}

async function finishDiagnosis() {
  showScreen('onboarding'); // Temp loading screen state
  document.querySelector('#screen-onboarding h1').textContent = 'Analyzing diagnostic quiz...';

  // Calculate scores
  let mathScore = 400;
  let rwScore = 400;

  if (state.diagnosticAnswers[0]) mathScore += 100;
  if (state.diagnosticAnswers[1]) mathScore += 100;
  if (state.diagnosticAnswers[2]) rwScore += 200;

  state.student.diagnosticScore = mathScore + rwScore;
  state.student.mathScore = mathScore;
  state.student.rwScore = rwScore;
  state.student.totalScore = mathScore + rwScore;

  // Insert initial weak areas based on thresholds
  state.weakAreas = [];
  if (mathScore < 550) {
    state.weakAreas.push({ category: "Linear Equations", section: "Math", proficiencyScore: 40 });
    state.weakAreas.push({ category: "Quadratic Equations", section: "Math", proficiencyScore: 30 });
  } else {
    state.weakAreas.push({ category: "Quadratic Equations", section: "Math", proficiencyScore: 65 });
  }

  if (rwScore < 550) {
    state.weakAreas.push({ category: "Inference", section: "Reading & Writing", proficiencyScore: 45 });
    state.weakAreas.push({ category: "Grammar & Punctuation", section: "Reading & Writing", proficiencyScore: 35 });
  } else {
    state.weakAreas.push({ category: "Inference", section: "Reading & Writing", proficiencyScore: 70 });
  }

  saveToStorage();

  // Call proxy server to generate study plan
  try {
    const response = await fetch(PROXY_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        messages: [
          {
            role: "system",
            content: "You are an expert SAT curriculum designer. Your job is to analyze a student's weak areas and output a structured study plan with EXACTLY 4 ordered steps in JSON format. The JSON must be an array of objects: [ { \"stepOrder\": 1, \"title\": \"Short title\", \"description\": \"Short explanation of what to review\", \"category\": \"The specific category\", \"estimatedMinutes\": 30 } ]. ONLY output valid JSON."
          },
          {
            role: "user",
            content: `Generate a 4-step study plan for weak areas: ${state.weakAreas.map(w => `${w.category} (${w.section})`).join(', ')}`
          }
        ]
      })
    });

    const data = await response.json();
    const planText = cleanJson(data.choices[0].message.content);
    state.studyPlan = JSON.parse(planText).map(step => ({ ...step, isCompleted: false }));
  } catch (err) {
    console.error('Failed to generate study plan via proxy, using fallback:', err);
    state.studyPlan = [
      { stepOrder: 1, title: "Master Linear Equations", description: "Review slope-intercept form and systems of equations.", category: "Linear Equations", estimatedMinutes: 30, isCompleted: false },
      { stepOrder: 2, title: "Quadratic Foundations", description: "Practice factoring, quadratic formula, and graphing.", category: "Quadratic Equations", estimatedMinutes: 45, isCompleted: false },
      { stepOrder: 3, title: "Inference Strategies", description: "Learn to identify logical conclusions in Reading passages.", category: "Inference", estimatedMinutes: 30, isCompleted: false },
      { stepOrder: 4, title: "Grammar Rules Mastery", description: "Focus on punctuation, semicolons, and pronoun agreement.", category: "Grammar & Punctuation", estimatedMinutes: 30, isCompleted: false }
    ];
  }

  // Restore onboarding title
  document.querySelector('#screen-onboarding h1').textContent = 'AceSAT AI Tutor';
  
  saveToStorage();
  showScreen('dashboard');
  renderDashboard();
}

// 2. Render Dashboard
function renderDashboard() {
  document.getElementById('dash-student-name').textContent = state.student.name;
  document.getElementById('dash-total-score').textContent = state.student.totalScore;
  document.getElementById('dash-math-score').textContent = `${state.student.mathScore}/800`;
  document.getElementById('dash-rw-score').textContent = `${state.student.rwScore}/800`;

  // Render weak areas
  const waContainer = document.getElementById('weak-areas-list');
  waContainer.innerHTML = '';
  if (state.weakAreas.length === 0) {
    waContainer.innerHTML = `<p style="font-style: italic;">No weak areas identified. Good job!</p>`;
  } else {
    state.weakAreas.forEach(wa => {
      const proficiency = wa.proficiencyScore;
      const scoreClass = proficiency < 50 ? 'low' : (proficiency < 75 ? 'mid' : 'high');
      
      const item = document.createElement('div');
      item.className = 'neobrutalist-card weak-area-item';
      item.innerHTML = `
        <div>
          <div class="wa-name">${wa.category}</div>
          <div class="wa-section">${wa.section}</div>
        </div>
        <div class="wa-score ${scoreClass}">${proficiency}%</div>
      `;
      waContainer.appendChild(item);
    });
  }

  // Render study plans
  const planContainer = document.getElementById('study-plan-list');
  planContainer.innerHTML = '';
  if (state.studyPlan.length === 0) {
    planContainer.innerHTML = `<p style="font-style: italic;">No study plan generated yet.</p>`;
  } else {
    state.studyPlan.forEach(step => {
      const item = document.createElement('div');
      item.className = `neobrutalist-card study-step-item ${step.isCompleted ? 'completed' : ''}`;
      item.innerHTML = `
        <div class="step-header">
          <span class="step-num-badge">Step ${step.stepOrder}</span>
          <span class="step-title">${step.title}</span>
        </div>
        <div class="step-desc">${step.description}</div>
        <div class="step-time">Estimated: ${step.estimatedMinutes} mins</div>
      `;
      planContainer.appendChild(item);
    });
  }

  // Render score progression log
  const logContainer = document.getElementById('progression-log-list');
  logContainer.innerHTML = '';
  if (state.attempts.length === 0) {
    logContainer.innerHTML = `<p style="font-style: italic;">Start practicing to see your attempts.</p>`;
  } else {
    state.attempts.slice().reverse().forEach(attempt => {
      const item = document.createElement('div');
      item.className = 'neobrutalist-card log-item';
      item.innerHTML = `
        <span class="log-icon">${attempt.isCorrect ? '✅' : '❌'}</span>
        <div class="log-info">
          <div class="log-cat">${attempt.category}</div>
          <div class="log-diff">Difficulty: ${attempt.difficulty}</div>
        </div>
        <div class="log-pts ${attempt.isCorrect ? 'plus' : 'minus'}">
          ${attempt.isCorrect ? '+10 pts' : '-10 pts'}
        </div>
      `;
      logContainer.appendChild(item);
    });
  }
}

// 3. Practice Flow (Adaptive Agent)
async function loadPracticeQuestion() {
  state.isPracticeSubmitted = false;
  state.selectedPracticeOption = null;
  state.currentPracticeQuestion = null;

  document.getElementById('explanation-container').classList.add('hidden');
  document.getElementById('btn-practice-next').classList.add('hidden');
  
  const submitBtn = document.getElementById('btn-practice-submit');
  submitBtn.classList.remove('hidden');
  submitBtn.classList.add('disabled');
  submitBtn.setAttribute('disabled', 'true');

  const questionTextElem = document.getElementById('practice-question-text');
  const optionsContainer = document.getElementById('practice-options');

  questionTextElem.textContent = 'Loading adaptive question from NVIDIA NIM...';
  optionsContainer.innerHTML = '';

  // Select target category (lowest proficiency score)
  let targetWa = state.weakAreas.reduce((min, wa) => wa.proficiencyScore < min.proficiencyScore ? wa : min, state.weakAreas[0]);
  const category = targetWa ? targetWa.category : "Linear Equations";
  const section = targetWa ? targetWa.section : "Math";

  // Adapt difficulty based on category history
  let difficulty = "Medium";
  const categoryAttempts = state.attempts.filter(a => a.category === category);
  if (categoryAttempts.length > 0) {
    const lastAttempt = categoryAttempts[categoryAttempts.length - 1];
    if (lastAttempt.isCorrect) {
      difficulty = lastAttempt.difficulty === "Easy" ? "Medium" : "Hard";
    } else {
      difficulty = lastAttempt.difficulty === "Hard" ? "Medium" : "Easy";
    }
  }

  document.getElementById('practice-category').textContent = category;
  document.getElementById('practice-difficulty').textContent = difficulty;

  try {
    const response = await fetch(PROXY_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        messages: [
          {
            role: "system",
            content: `You are an adaptive SAT tutor. Generate a ${difficulty} level multiple-choice question for category "${category}" in ${section} section. The response must be raw JSON with exactly these keys: { "question": "Question text", "options": { "A": "Text", "B": "Text", "C": "Text", "D": "Text" }, "correctAnswer": "A", "explanation": "Brief explanation" }. ONLY output valid JSON.`
          },
          {
            role: "user",
            content: `Generate a ${difficulty} question for: ${category}`
          }
        ]
      })
    });

    const data = await response.json();
    const rawContent = cleanJson(data.choices[0].message.content);
    state.currentPracticeQuestion = {
      ...JSON.parse(rawContent),
      category,
      section,
      difficulty
    };

    renderPracticeQuestion();
  } catch (err) {
    console.error('Failed to load adaptive question from proxy, using local mock:', err);
    // Offline local fallback questions
    const fallbackQ = {
      question: `Solve for x in this ${difficulty} level ${category} question: 3x - 7 = 14. What is the value of x?`,
      options: { "A": "5", "B": "7", "C": "6", "D": "8" },
      correctAnswer: "B",
      explanation: "Add 7 to both sides: 3x = 21. Dividing by 3 yields x = 7.",
      category,
      section,
      difficulty
    };
    state.currentPracticeQuestion = fallbackQ;
    renderPracticeQuestion();
  }
}

function renderPracticeQuestion() {
  const q = state.currentPracticeQuestion;
  document.getElementById('practice-question-text').textContent = q.question;

  const optionsContainer = document.getElementById('practice-options');
  optionsContainer.innerHTML = '';

  Object.entries(q.options).forEach(([key, value]) => {
    const optionBtn = document.createElement('div');
    optionBtn.className = 'option-item';
    optionBtn.textContent = `${key}. ${value}`;
    optionBtn.dataset.option = key;

    optionBtn.addEventListener('click', () => {
      if (state.isPracticeSubmitted) return;
      
      document.querySelectorAll('#practice-options .option-item').forEach(b => b.classList.remove('selected'));
      optionBtn.classList.add('selected');
      state.selectedPracticeOption = key;

      const submitBtn = document.getElementById('btn-practice-submit');
      submitBtn.classList.remove('disabled');
      submitBtn.removeAttribute('disabled');
    });

    optionsContainer.appendChild(optionBtn);
  });
}

function submitPracticeAnswer() {
  state.isPracticeSubmitted = true;
  const q = state.currentPracticeQuestion;
  const selected = state.selectedPracticeOption;
  const isCorrect = selected === q.correctAnswer;

  // Render answer states
  document.querySelectorAll('#practice-options .option-item').forEach(btn => {
    const opt = btn.dataset.option;
    if (opt === q.correctAnswer) {
      btn.className = 'option-item correct';
    } else if (opt === selected && !isCorrect) {
      btn.className = 'option-item wrong';
    } else {
      btn.className = 'option-item';
    }
  });

  // Show explanation
  document.getElementById('explanation-text').textContent = q.explanation;
  document.getElementById('explanation-container').classList.remove('hidden');

  // Toggle buttons
  document.getElementById('btn-practice-submit').classList.add('hidden');
  document.getElementById('btn-practice-next').classList.remove('hidden');

  // Record attempt
  state.attempts.push({
    category: q.category,
    difficulty: q.difficulty,
    isCorrect: isCorrect
  });

  // Adapt proficiency score
  let wa = state.weakAreas.find(w => w.category === q.category);
  if (wa) {
    let scoreChange = isCorrect ? 10 : -7;
    if (q.difficulty === "Easy" && isCorrect) scoreChange = 5;
    if (q.difficulty === "Hard" && isCorrect) scoreChange = 15;
    if (q.difficulty === "Easy" && !isCorrect) scoreChange = -10;
    if (q.difficulty === "Hard" && !isCorrect) scoreChange = -4;

    wa.proficiencyScore = Math.max(10, Math.min(100, wa.proficiencyScore + scoreChange));
  }

  // Adjust scores
  const scoreChange = isCorrect ? 10 : -10;
  if (q.section === "Math") {
    state.student.mathScore = Math.max(200, Math.min(800, state.student.mathScore + scoreChange));
  } else {
    state.student.rwScore = Math.max(200, Math.min(800, state.student.rwScore + scoreChange));
  }
  state.student.totalScore = state.student.mathScore + state.student.rwScore;

  // Complete study step if proficiency is high
  if (wa && wa.proficiencyScore >= 80) {
    let step = state.studyPlan.find(s => s.category === q.category);
    if (step) step.isCompleted = true;
  }

  saveToStorage();
}

// Helpers
function cleanJson(raw) {
  let clean = raw.trim();
  if (clean.startsWith('```json')) {
    clean = clean.substring(7);
  } else if (clean.startsWith('```')) {
    clean = clean.substring(3);
  }
  if (clean.endsWith('```')) {
    clean = clean.substring(0, clean.length - 3);
  }
  return clean.trim();
}
