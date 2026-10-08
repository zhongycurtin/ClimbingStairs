// StairStep Web Interactive Engine

const STORAGE_KEY_TRIPS = "stairstep_trips_v1";
const STORAGE_KEY_WEIGHTS = "stairstep_weights_v1";
const STORAGE_KEY_SETTINGS = "stairstep_settings_v1";

// State
let settings = {
  floorsPerLap: 18,
  currentWeight: 65.0
};

let trips = [];
let weights = [];
let isTiming = false;
let timerStartMillis = 0;
let timerInterval = null;
let currentTab = "home";
let selectedStatsRange = 7;

// Initialize
function init() {
  loadData();
  setupSampleDataIfEmpty();
  setupEventListeners();
  updateClock();
  setInterval(updateClock, 1000);
  renderAll();
}

function loadData() {
  try {
    const savedSettings = localStorage.getItem(STORAGE_KEY_SETTINGS);
    if (savedSettings) settings = JSON.parse(savedSettings);

    const savedTrips = localStorage.getItem(STORAGE_KEY_TRIPS);
    if (savedTrips) trips = JSON.parse(savedTrips);

    const savedWeights = localStorage.getItem(STORAGE_KEY_WEIGHTS);
    if (savedWeights) weights = JSON.parse(savedWeights);
  } catch (e) {
    console.error("Failed to load local storage", e);
  }
}

function saveData() {
  localStorage.setItem(STORAGE_KEY_SETTINGS, JSON.stringify(settings));
  localStorage.setItem(STORAGE_KEY_TRIPS, JSON.stringify(trips));
  localStorage.setItem(STORAGE_KEY_WEIGHTS, JSON.stringify(weights));
}

// 首次体验默认准备几条逼真记录展示设计感
function setupSampleDataIfEmpty() {
  const today = getTodayDateStr();
  if (trips.length === 0 && weights.length === 0) {
    // 写入体重序列
    const d1 = getDateStrOffset(-5);
    const d2 = getDateStrOffset(-3);
    const d3 = getDateStrOffset(-1);
    weights = [
      { id: 1, date: d1, weight: 66.8, timestamp: Date.now() - 5 * 86400000 },
      { id: 2, date: d2, weight: 66.3, timestamp: Date.now() - 3 * 86400000 },
      { id: 3, date: d3, weight: 65.8, timestamp: Date.now() - 1 * 86400000 },
      { id: 4, date: today, weight: 65.2, timestamp: Date.now() }
    ];
    settings.currentWeight = 65.2;

    // 写入历史爬楼
    trips = [
      {
        id: 101,
        date: d1,
        startTime: Date.now() - 5 * 86400000,
        endTime: Date.now() - 5 * 86400000 + 150000,
        durationSeconds: 150,
        floors: 18,
        calories: 32.5,
        tag: "晚间燃脂",
        isBackfill: false
      },
      {
        id: 102,
        date: d2,
        startTime: Date.now() - 3 * 86400000,
        endTime: Date.now() - 3 * 86400000 + 320000,
        durationSeconds: 320,
        floors: 36,
        calories: 68.0,
        tag: "历史补录",
        isBackfill: true
      },
      {
        id: 103,
        date: d3,
        startTime: Date.now() - 1 * 86400000,
        endTime: Date.now() - 1 * 86400000 + 140000,
        durationSeconds: 140,
        floors: 18,
        calories: 31.8,
        tag: "白天摸鱼",
        isBackfill: false
      }
    ];
    saveData();
  }
}

function getTodayDateStr() {
  const d = new Date();
  return d.toISOString().split("T")[0];
}

function getDateStrOffset(offsetDays) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split("T")[0];
}

function formatDuration(sec) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  if (m > 0) return `${m}分${s}秒`;
  return `${s}秒`;
}

function calculateCalories(durationSeconds, floors, weight) {
  // 爬楼 MET=8.8
  const calByTime = 8.8 * weight * (durationSeconds / 3600);
  const calByFloors = floors * 0.14 * (weight / 60);
  return Math.max(calByTime, calByFloors);
}

function determineAutoTag() {
  const h = new Date().getHours();
  if (h >= 9 && h <= 17) return "白天摸鱼";
  if (h >= 18 && h <= 23) return "晚间燃脂";
  if (h >= 5 && h <= 8) return "晨间唤醒";
  return "深夜攀爬";
}

// 核心打卡操作
function startTimer() {
  isTiming = true;
  timerStartMillis = Date.now();
  document.getElementById("heroButton").classList.add("timing");
  document.getElementById("heroBtnInner").classList.add("timing");
  document.getElementById("idleBtnContent").style.display = "none";
  document.getElementById("timingBtnContent").style.display = "block";
  document.getElementById("quickAddBtn").style.display = "none";
  document.getElementById("cancelTimerBtn").style.display = "block";

  updateLiveTimer();
  timerInterval = setInterval(updateLiveTimer, 500);
}

function updateLiveTimer() {
  const elapsed = Math.floor((Date.now() - timerStartMillis) / 1000);
  const m = Math.floor(elapsed / 60).toString().padStart(2, "0");
  const s = (elapsed % 60).toString().padStart(2, "0");
  document.getElementById("liveTimerText").innerText = `${m}:${s}`;
}

function finishTimer() {
  if (!isTiming) return;
  const duration = Math.max(1, Math.floor((Date.now() - timerStartMillis) / 1000));
  const startTime = timerStartMillis;
  const endTime = Date.now();
  const floors = settings.floorsPerLap;
  const cal = calculateCalories(duration, floors, settings.currentWeight);
  const tag = determineAutoTag();

  trips.unshift({
    id: Date.now(),
    date: getTodayDateStr(),
    startTime,
    endTime,
    durationSeconds: duration,
    floors,
    calories: cal,
    tag,
    isBackfill: false
  });

  resetTimer();
  saveData();
  renderAll();
}

function cancelTimer() {
  resetTimer();
}

function resetTimer() {
  isTiming = false;
  clearInterval(timerInterval);
  timerInterval = null;
  document.getElementById("heroButton").classList.remove("timing");
  document.getElementById("heroBtnInner").classList.remove("timing");
  document.getElementById("idleBtnContent").style.display = "block";
  document.getElementById("timingBtnContent").style.display = "none";
  document.getElementById("quickAddBtn").style.display = "flex";
  document.getElementById("cancelTimerBtn").style.display = "none";
}

function quickAddTrip() {
  const today = getTodayDateStr();
  const todayTrips = trips.filter(t => t.date === today);
  const avg = todayTrips.length > 0 
    ? Math.round(todayTrips.reduce((a, b) => a + b.durationSeconds, 0) / todayTrips.length) 
    : Math.max(30, settings.floorsPerLap * 7);

  const now = Date.now();
  const startTime = now - avg * 1000;
  const floors = settings.floorsPerLap;
  const cal = calculateCalories(avg, floors, settings.currentWeight);
  const tag = determineAutoTag();

  trips.unshift({
    id: Date.now(),
    date: today,
    startTime,
    endTime: now,
    durationSeconds: avg,
    floors,
    calories: cal,
    tag,
    isBackfill: false
  });

  saveData();
  renderAll();
}

function backfillTrip(date, floors, weight) {
  if (weight && weight > 0) {
    weights.push({
      id: Date.now() + 1,
      date,
      weight: parseFloat(weight),
      timestamp: new Date(date).getTime()
    });
    weights.sort((a, b) => a.date.localeCompare(b.date));
    settings.currentWeight = parseFloat(weight);
  }

  const duration = Math.max(60, floors * 7);
  const cal = calculateCalories(duration, floors, settings.currentWeight);
  const fakeTime = new Date(`${date}T18:00:00`).getTime();

  trips.unshift({
    id: Date.now(),
    date,
    startTime: fakeTime,
    endTime: fakeTime + duration * 1000,
    durationSeconds: duration,
    floors,
    calories: cal,
    tag: "历史补录",
    isBackfill: true
  });

  saveData();
  renderAll();
}

function deleteTrip(id) {
  trips = trips.filter(t => t.id !== id);
  saveData();
  renderAll();
}

// 页面渲染
function renderAll() {
  renderHome();
  renderStats();
}

function renderHome() {
  const today = getTodayDateStr();
  const todayTrips = trips.filter(t => t.date === today);

  document.getElementById("currentWeightDisplay").innerText = settings.currentWeight.toFixed(1);

  const laps = todayTrips.length;
  const floors = todayTrips.reduce((acc, t) => acc + t.floors, 0);
  const totalSeconds = todayTrips.reduce((acc, t) => acc + t.durationSeconds, 0);
  const avgSeconds = laps > 0 ? Math.round(totalSeconds / laps) : 0;
  const calories = todayTrips.reduce((acc, t) => acc + t.calories, 0);

  document.getElementById("todayTripsCount").innerText = laps;
  document.getElementById("todayFloorsSub").innerText = `单趟 ${settings.floorsPerLap} 层 (累计 ${floors} 层)`;
  document.getElementById("todayCalories").innerText = Math.round(calories);
  document.getElementById("todayElevationSub").innerText = `垂直攀登约 ${floors * 3} 米`;
  document.getElementById("todayTotalDuration").innerText = formatDuration(totalSeconds);
  document.getElementById("todayAvgDuration").innerText = laps > 0 ? formatDuration(avgSeconds) : "--";
  document.getElementById("tripCountBadge").innerText = `${laps} 趟`;

  // 流水列表
  const listEl = document.getElementById("timelineList");
  if (todayTrips.length === 0) {
    listEl.innerHTML = `<div class="empty-state">今日尚未开爬，轻触大圆钮开启第一趟吧！</div>`;
  } else {
    listEl.innerHTML = todayTrips.map((trip, idx) => {
      const tripNum = todayTrips.length - idx;
      const timeStr = new Date(trip.startTime).toTimeString().substring(0, 5);
      const tagClass = trip.tag === "白天摸鱼" ? "tag-day" : (trip.tag === "历史补录" ? "tag-backfill" : "tag-night");
      return `
        <div class="timeline-item">
          <div style="display:flex; align-items:center;">
            <div class="trip-badge-num">#${tripNum}</div>
            <div class="trip-info">
              <div class="trip-top-row">
                <span class="trip-time">${timeStr}</span>
                <span class="trip-tag ${tagClass}">${trip.tag}</span>
              </div>
              <div class="trip-meta">
                <span>⏱ 耗时 ${formatDuration(trip.durationSeconds)}</span>
                <span>🔥 ${trip.calories.toFixed(1)} kcal</span>
              </div>
            </div>
          </div>
          <div class="trip-right">
            <span class="trip-floors">${trip.floors}层</span>
            <button class="del-btn" onclick="deleteTrip(${trip.id})" title="删除">✕</button>
          </div>
        </div>
      `;
    }).join("");
  }
}

function renderStats() {
  const totalFloors = trips.reduce((acc, t) => acc + t.floors, 0);
  const totalCalories = trips.reduce((acc, t) => acc + t.calories, 0);
  const totalMeters = totalFloors * 3.0;
  const fatLossGrams = totalCalories / 7.7;

  document.getElementById("cumulativeHeightMeters").innerText = Math.round(totalMeters);
  document.getElementById("cumulativeFloorsSub").innerText = `相当于 ${totalFloors} 层楼`;
  document.getElementById("cumulativeFatLossGrams").innerText = Math.round(fatLossGrams);
  document.getElementById("cumulativeCaloriesSub").innerText = `累计耗能 ${Math.round(totalCalories)} kcal`;
  document.getElementById("statsCurrentWeight").innerText = `${settings.currentWeight.toFixed(1)} kg`;

  // 渲染地标里程碑
  renderMilestones(totalMeters);

  // 绘制柱状图与体重曲线
  drawActivityChart();
  drawWeightChart();
}

function renderMilestones(currentMeters) {
  const landmarks = [
    { name: "埃菲尔铁塔", height: 330 },
    { name: "东方明珠塔", height: 468 },
    { name: "台北 101", height: 508 },
    { name: "广州塔小蛮腰", height: 600 },
    { name: "哈利法塔", height: 828 },
    { name: "泰山之巅", height: 1545 }
  ];

  const html = landmarks.map(lm => {
    const pct = Math.min(100, Math.round((currentMeters / lm.height) * 100));
    const completed = currentMeters >= lm.height;
    return `
      <div class="milestone-item">
        <div class="milestone-top">
          <span>${completed ? "✅ " : "🏔️ "}${lm.name}</span>
          <span style="color:var(--text-secondary);">${lm.height} 米 (${pct}%)</span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" style="width: ${pct}%;"></div>
        </div>
      </div>
    `;
  }).join("");

  document.getElementById("milestonesList").innerHTML = html;
}

// 绘制每日爬升柱状图
function drawActivityChart() {
  const canvas = document.getElementById("activityCanvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;

  canvas.width = canvas.clientWidth * dpr;
  canvas.height = canvas.clientHeight * dpr;
  ctx.scale(dpr, dpr);

  const w = canvas.clientWidth;
  const h = canvas.clientHeight;
  ctx.clearRect(0, 0, w, h);

  const days = selectedStatsRange;
  const data = [];
  for (let i = days - 1; i >= 0; i--) {
    const dateStr = getDateStrOffset(-i);
    const dayTrips = trips.filter(t => t.date === dateStr);
    const floors = dayTrips.reduce((acc, t) => acc + t.floors, 0);
    const label = dateStr.substring(5);
    data.push({ date: dateStr, label, floors });
  }

  const maxFloors = Math.max(1, ...data.map(d => d.floors));
  const slotW = w / days;
  const barW = Math.min(22, slotW * 0.55);

  data.forEach((d, idx) => {
    const x = idx * slotW + (slotW - barW) / 2;
    const barH = Math.max(4, (d.floors / maxFloors) * (h - 30));
    const y = h - 22 - barH;

    // 柱子圆角
    ctx.fillStyle = idx === days - 1 ? "#10b981" : "rgba(16, 185, 129, 0.35)";
    roundRect(ctx, x, y, barW, barH, 4);
    ctx.fill();

    // 日期文本
    ctx.fillStyle = "#94a3b8";
    ctx.font = "10px sans-serif";
    ctx.textAlign = "center";
    if (days <= 10 || idx % 4 === 0 || idx === days - 1) {
      ctx.fillText(d.label, x + barW / 2, h - 6);
    }
  });
}

// 绘制体重折线图 (平滑贝塞尔曲线)
function drawWeightChart() {
  const canvas = document.getElementById("weightCanvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;

  canvas.width = canvas.clientWidth * dpr;
  canvas.height = canvas.clientHeight * dpr;
  ctx.scale(dpr, dpr);

  const w = canvas.clientWidth;
  const h = canvas.clientHeight;
  ctx.clearRect(0, 0, w, h);

  if (weights.length < 2) {
    ctx.fillStyle = "#94a3b8";
    ctx.font = "12px sans-serif";
    ctx.textAlign = "center";
    ctx.fillText("暂无或仅有 1 次体重记录", w / 2, h / 2);
    return;
  }

  const sortedWeights = [...weights].sort((a, b) => a.date.localeCompare(b.date));
  const minW = Math.min(...sortedWeights.map(w => w.weight)) - 0.5;
  const maxW = Math.max(...sortedWeights.map(w => w.weight)) + 0.5;
  const range = Math.max(0.2, maxW - minW);

  const stepX = (w - 30) / (sortedWeights.length - 1);
  const points = sortedWeights.map((item, idx) => {
    const x = 15 + idx * stepX;
    const y = (h - 25) - ((item.weight - minW) / range) * (h - 45);
    return { x, y, weight: item.weight, date: item.date.substring(5) };
  });

  // 渐变填充背景
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  for (let i = 0; i < points.length - 1; i++) {
    const cx = (points[i].x + points[i + 1].x) / 2;
    ctx.bezierCurveTo(cx, points[i].y, cx, points[i + 1].y, points[i + 1].x, points[i + 1].y);
  }
  ctx.lineTo(points[points.length - 1].x, h);
  ctx.lineTo(points[0].x, h);
  ctx.closePath();

  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, "rgba(16, 185, 129, 0.25)");
  grad.addColorStop(1, "rgba(16, 185, 129, 0.0)");
  ctx.fillStyle = grad;
  ctx.fill();

  // 曲线本体
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  for (let i = 0; i < points.length - 1; i++) {
    const cx = (points[i].x + points[i + 1].x) / 2;
    ctx.bezierCurveTo(cx, points[i].y, cx, points[i + 1].y, points[i + 1].x, points[i + 1].y);
  }
  ctx.strokeStyle = "#10b981";
  ctx.lineWidth = 2.5;
  ctx.stroke();

  // 节点实心圆
  points.forEach(p => {
    ctx.beginPath();
    ctx.arc(p.x, p.y, 3.5, 0, Math.PI * 2);
    ctx.fillStyle = "#ffffff";
    ctx.fill();
    ctx.strokeStyle = "#10b981";
    ctx.lineWidth = 2;
    ctx.stroke();
  });
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

// 导出 CSV
function exportCsv() {
  let csv = "\uFEFF# 步步登峰 - 爬楼运动记录\nID,日期,开始时间,结束时间,耗时(秒),爬升楼层,估算卡路里(kcal),标签,是否补录\n";
  trips.forEach(t => {
    const sStr = new Date(t.startTime).toLocaleString();
    const eStr = new Date(t.endTime).toLocaleString();
    csv += `${t.id},"${t.date}","${sStr}","${eStr}",${t.durationSeconds},${t.floors},${t.calories.toFixed(1)},"${t.tag}",${t.isBackfill ? "是" : "否"}\n`;
  });

  csv += "\n# 步步登峰 - 体重追踪历史\nID,记录日期,记录时间,体重(kg)\n";
  weights.forEach(w => {
    const tStr = new Date(w.timestamp).toLocaleString();
    csv += `${w.id},"${w.date}","${tStr}",${w.weight.toFixed(1)}\n`;
  });

  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `stairstep_data_${getTodayDateStr()}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function updateClock() {
  const d = new Date();
  const h = d.getHours().toString().padStart(2, "0");
  const m = d.getMinutes().toString().padStart(2, "0");
  document.getElementById("systemClock").innerText = `${h}:${m}`;
}

// 事件绑定
function setupEventListeners() {
  // Tab 切换
  document.getElementById("tabHome").onclick = () => switchTab("home");
  document.getElementById("tabStats").onclick = () => switchTab("stats");

  // 核心大圆钮 (开始 / 完成)
  document.getElementById("heroButton").onclick = () => {
    if (!isTiming) {
      startTimer();
    } else {
      finishTimer();
    }
  };

  // 摸鱼快捷 +1
  document.getElementById("quickAddBtn").onclick = () => quickAddTrip();
  document.getElementById("cancelTimerBtn").onclick = () => cancelTimer();

  // 周期切换
  document.getElementById("range7Btn").onclick = () => {
    selectedStatsRange = 7;
    document.getElementById("range7Btn").classList.add("active");
    document.getElementById("range30Btn").classList.remove("active");
    drawActivityChart();
  };
  document.getElementById("range30Btn").onclick = () => {
    selectedStatsRange = 30;
    document.getElementById("range30Btn").classList.add("active");
    document.getElementById("range7Btn").classList.remove("active");
    drawActivityChart();
  };

  // 导出 CSV
  document.getElementById("exportCsvBtn").onclick = () => exportCsv();

  // 单趟楼层弹窗
  document.getElementById("openSettingsBtn").onclick = () => {
    document.getElementById("inputFloors").value = settings.floorsPerLap;
    document.getElementById("floorModal").classList.add("open");
  };
  document.getElementById("closeFloorModal").onclick = () => {
    document.getElementById("floorModal").classList.remove("open");
  };
  document.getElementById("saveFloorModal").onclick = () => {
    const val = parseInt(document.getElementById("inputFloors").value);
    if (val && val > 0) {
      settings.floorsPerLap = val;
      saveData();
      renderAll();
    }
    document.getElementById("floorModal").classList.remove("open");
  };

  // 体重弹窗
  document.getElementById("openWeightBtn").onclick = () => {
    document.getElementById("inputWeight").value = settings.currentWeight.toFixed(1);
    document.getElementById("weightModal").classList.add("open");
  };
  document.getElementById("closeWeightModal").onclick = () => {
    document.getElementById("weightModal").classList.remove("open");
  };
  document.getElementById("saveWeightModal").onclick = () => {
    const val = parseFloat(document.getElementById("inputWeight").value);
    if (val && val > 0) {
      settings.currentWeight = val;
      const today = getTodayDateStr();
      weights.push({
        id: Date.now(),
        date: today,
        weight: val,
        timestamp: Date.now()
      });
      weights.sort((a, b) => a.date.localeCompare(b.date));
      saveData();
      renderAll();
    }
    document.getElementById("weightModal").classList.remove("open");
  };

  // 漏记补录弹窗
  document.getElementById("openBackfillBtn").onclick = () => {
    document.getElementById("inputBackfillDate").value = getDateStrOffset(-1);
    document.getElementById("inputBackfillFloors").value = settings.floorsPerLap;
    document.getElementById("inputBackfillWeight").value = settings.currentWeight.toFixed(1);
    document.getElementById("backfillModal").classList.add("open");
  };
  document.getElementById("closeBackfillModal").onclick = () => {
    document.getElementById("backfillModal").classList.remove("open");
  };
  document.getElementById("saveBackfillModal").onclick = () => {
    const date = document.getElementById("inputBackfillDate").value;
    const fl = parseInt(document.getElementById("inputBackfillFloors").value);
    const w = parseFloat(document.getElementById("inputBackfillWeight").value);
    if (date && fl > 0) {
      backfillTrip(date, fl, isNaN(w) ? null : w);
    }
    document.getElementById("backfillModal").classList.remove("open");
  };
}

function switchTab(tab) {
  currentTab = tab;
  if (tab === "home") {
    document.getElementById("homePage").style.display = "block";
    document.getElementById("statsPage").style.display = "none";
    document.getElementById("tabHome").classList.add("active");
    document.getElementById("tabStats").classList.remove("active");
    renderHome();
  } else {
    document.getElementById("homePage").style.display = "none";
    document.getElementById("statsPage").style.display = "block";
    document.getElementById("tabHome").classList.remove("active");
    document.getElementById("tabStats").classList.add("active");
    renderStats();
  }
}

// 启动应用
window.onload = init;
