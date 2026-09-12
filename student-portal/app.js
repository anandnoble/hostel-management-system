/* ═══════════════════════════════════════════════════════════════
   app.js — Streamlined Student Web Portal Logic
   Hostel Management System · Supabase + QRCode.js + Encrypted RPC
  ═══════════════════════════════════════════════════════════════ */

'use strict';

// ── Supabase Configuration ─────────────────────────────────────
const SUPABASE_URL      = 'https://jtxcqovfakumoaxgwumt.supabase.co';
const SUPABASE_ANON_KEY  = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp0eGNxb3ZmYWt1bW9heGd3dW10Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgwNjE1MTYsImV4cCI6MjEwMzYzNzUxNn0.FmE7Ay0ere3u-2vG9VaMYQ1OZfviqmcIWW6pe5yZqJ4';

const sb = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ── State Variables ────────────────────────────────────────────
let hostelId           = null;
let hostelData         = null;
let paymentConfig      = null; // { upi_id, monthly_fee, advance_deposit }
let currentUser        = null;
let currentRegistration = null;

let floorsList         = [];
let roomsList          = [];
let bedsList           = [];

let selectedFloorId    = null;
let selectedRoomId     = null;
let selectedBedId      = null;

// ── Helper Functions ───────────────────────────────────────────
const $ = id => document.getElementById(id);

const showScreen = name => {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const target = $(`screen-${name}`);
  if (target) target.classList.add('active');
};

const toast = (msg, type = '') => {
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = msg;
  document.body.appendChild(el);
  requestAnimationFrame(() => {
    el.classList.add('show');
    setTimeout(() => {
      el.classList.remove('show');
      setTimeout(() => el.remove(), 400);
    }, 3000);
  });
};

// ── Initialise Application & URL Router ────────────────────────
document.addEventListener('DOMContentLoaded', init);

async function init() {
  showScreen('loading');

  // Parse URL parameters for QR routing (e.g. ?hostel_id=UUID or ?hostel=UUID)
  const urlParams = new URLSearchParams(window.location.search);
  hostelId = urlParams.get('hostel_id') || urlParams.get('hostelId') || urlParams.get('hostel');

  // Load Hostel details & Payment config if hostelId present
  if (hostelId) {
    await fetchHostelDetails(hostelId);
  } else {
    // Fallback: If no hostel_id in URL, fetch first hostel from database so payment settings are loaded
    try {
      const { data: firstHostel } = await sb
        .from('hostels')
        .select('id')
        .limit(1)
        .maybeSingle();

      if (firstHostel && firstHostel.id) {
        hostelId = firstHostel.id;
        await fetchHostelDetails(hostelId);
      }
    } catch (e) {
      console.warn('Fallback hostel fetch error:', e);
    }
  }

  // Check Supabase Auth Session
  const { data: { session } } = await sb.auth.getSession();
  if (session && session.user) {
    currentUser = session.user;
    await routeAuthenticatedStudent();
    return;
  }

  // If opened via QR code with hostel_id, default directly to Registration
  if (hostelId) {
    showAuthMode('register');
  } else {
    showAuthMode('login');
  }
}

async function fetchHostelDetails(id) {
  try {
    let hostel = null;

    // 1. Try querying hostel with non-empty payment config
    if (id) {
      const resp = await sb
        .from('hostels')
        .select('id, name, address, upi_id, monthly_fee, advance_deposit')
        .eq('id', id)
        .maybeSingle();
      if (resp.data && (resp.data.upi_id || resp.data.monthly_fee > 0)) {
        hostel = resp.data;
      }
    }

    // 2. If specific ID has empty fields or no ID provided, load the latest hostel row with valid payment config
    if (!hostel) {
      const resp = await sb
        .from('hostels')
        .select('id, name, address, upi_id, monthly_fee, advance_deposit')
        .not('upi_id', 'is', null)
        .not('upi_id', 'eq', '')
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();
      hostel = resp.data;
    }

    // 3. Ultimate fallback: load any hostel row
    if (!hostel) {
      const resp = await sb
        .from('hostels')
        .select('id, name, address, upi_id, monthly_fee, advance_deposit')
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();
      hostel = resp.data;
    }

    if (hostel) {
      hostelData = hostel;
      if (hostel.id) hostelId = hostel.id;
      if ($('nav-hostel-name')) $('nav-hostel-name').textContent = hostel.name || 'Hostel';
      if ($('booking-hostel-name')) $('booking-hostel-name').textContent = `Book your bed at ${hostel.name || 'Hostel'}.`;

      paymentConfig = {
        upi_id: hostel.upi_id || '',
        monthly_fee: hostel.monthly_fee || 0,
        advance_deposit: hostel.advance_deposit || 0
      };
      console.log('Payment config loaded successfully:', paymentConfig);
    }
  } catch (e) {
    console.warn('Error fetching hostel details:', e);
  }
}

// ── Auth Mode Switcher ─────────────────────────────────────────
function showAuthMode(mode) {
  showScreen('auth');
  if (mode === 'register') {
    $('tab-register').classList.add('active');
    $('tab-login').classList.remove('active');
    $('form-register').style.display = 'block';
    $('form-login').style.display = 'none';
    $('auth-title').textContent = hostelData ? `Register for ${hostelData.name}` : 'Student Registration';
    $('auth-subtitle').textContent = 'Create your account to choose your room & bed';
    $('nav-btn-register').classList.add('active');
    $('nav-btn-login').classList.remove('active');
  } else {
    $('tab-login').classList.add('active');
    $('tab-register').classList.remove('active');
    $('form-login').style.display = 'block';
    $('form-register').style.display = 'none';
    $('auth-title').textContent = 'Student Portal Login';
    $('auth-subtitle').textContent = 'Access your allocated bed & hostel announcements';
    $('nav-btn-login').classList.add('active');
    $('nav-btn-register').classList.remove('active');
  }
}

// ── Registration Handler ───────────────────────────────────────
async function handleRegister() {
  const fullName = $('reg-name').value.trim();
  const email    = $('reg-email').value.trim();
  const phone    = $('reg-phone').value.trim();
  const password = $('reg-password').value.trim();

  if (!fullName || !email || !password || !phone) {
    toast('Please fill in all required registration fields', 'error');
    return;
  }

  const btn = $('btn-do-register');
  btn.disabled = true;
  btn.textContent = 'Registering…';

  try {
    const { data, error } = await sb.auth.signUp({
      email,
      password,
      options: {
        data: {
          full_name: fullName,
          role: 'STUDENT',
          phone: phone,
          hostel_id: hostelId || null
        }
      }
    });

    if (error && !error.message.includes('User already registered')) {
      throw error;
    }

    // Auto sign in after registration
    const { data: loginData, error: loginErr } = await sb.auth.signInWithPassword({ email, password });
    if (loginErr) throw loginErr;

    currentUser = loginData.user;
    toast('Registration successful!', 'success');
    await routeAuthenticatedStudent();
  } catch (err) {
    console.error('Registration error:', err);
    toast(err.message || 'Registration failed', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '🎓   Register Account';
  }
}

// ── Login Handler ──────────────────────────────────────────────
async function handleLogin() {
  const email    = $('login-email').value.trim();
  const password = $('login-password').value.trim();

  if (!email || !password) {
    toast('Please enter your email and password', 'error');
    return;
  }

  const btn = $('btn-do-login');
  btn.disabled = true;
  btn.textContent = 'Logging in…';

  try {
    const { data, error } = await sb.auth.signInWithPassword({ email, password });
    if (error) throw error;

    currentUser = data.user;
    toast('Login successful!', 'success');
    await routeAuthenticatedStudent();
  } catch (err) {
    console.error('Login error:', err);
    toast(err.message || 'Incorrect email or password', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '🔑   Log In to Portal';
  }
}

async function handleLogout() {
  await sb.auth.signOut();
  currentUser = null;
  currentRegistration = null;
  $('user-badge-wrap').style.display = 'none';
  $('nav-btn-register').style.display = 'inline-block';
  $('nav-btn-login').style.display = 'inline-block';
  showAuthMode('login');
  toast('Logged out', 'success');
}

// ── Route Authenticated Student ────────────────────────────────
async function routeAuthenticatedStudent() {
  $('nav-btn-register').style.display = 'none';
  $('nav-btn-login').style.display = 'none';
  $('user-badge-wrap').style.display = 'flex';
  $('user-profile-name').textContent = currentUser.email;

  // Check existing self-registration record for this student (latest record)
  const { data: regs } = await sb
    .from('student_self_registrations')
    .select('*')
    .eq('email', currentUser.email)
    .order('created_at', { ascending: false })
    .limit(1);

  const reg = (regs && regs.length > 0) ? regs[0] : null;
  currentRegistration = reg;

  if (reg) {
    if (reg.hostel_id && !hostelId) {
      hostelId = reg.hostel_id;
      await fetchHostelDetails(hostelId);
    }

    if (reg.status === 'Approved') {
      // Approved by hostel owner -> Show clean Approved Dashboard
      await renderApprovedDashboard();
    } else {
      // Submitted but pending verification -> Show Verification Banner
      renderUnderVerificationScreen();
    }
  } else {
    // New user -> Show Bed Selection & Payment Screen
    await setupBookingScreen();
  }
}

// ── SETUP BED SELECTION & ADVANCE PAYMENT ──────────────────────
async function setupBookingScreen() {
  showScreen('booking');

  // Fetch payment details directly from Supabase for this hostel ID if not already loaded
  if (!paymentConfig) {
    await fetchHostelDetails(hostelId);
  }

  // Display payment details strictly from Supabase for this specific hostel
  const upiId          = paymentConfig?.upi_id ? paymentConfig.upi_id : 'Pending Hostel Setup';
  const monthlyVal     = paymentConfig?.monthly_fee ? parseFloat(paymentConfig.monthly_fee) : 0;
  const advanceDeposit = paymentConfig?.advance_deposit ? parseFloat(paymentConfig.advance_deposit) : 0;

  const monthlyFeeStr  = `₹${monthlyVal.toFixed(2)}`;
  const advanceFeeStr  = `₹${advanceDeposit.toFixed(2)}`;

  $('display-upi-id').textContent       = upiId;
  $('display-monthly-fee').textContent  = `${monthlyFeeStr} / month`;
  $('display-advance-fee').textContent  = advanceFeeStr;
  $('display-advance-amount').textContent = advanceFeeStr;

  // Set default joining date to today
  const today = new Date().toISOString().split('T')[0];
  $('input-joining-date').value = today;

  // Pre-fill student name and phone if available
  if ($('input-full-name') && !$('input-full-name').value) {
    $('input-full-name').value = currentUser.user_metadata?.full_name || currentRegistration?.full_name || '';
  }
  if ($('input-phone') && !$('input-phone').value) {
    $('input-phone').value = currentUser.user_metadata?.phone || currentRegistration?.phone || '';
  }

  // Load Floors for the Hostel
  await loadFloors();

  // Generate UPI QR Code pre-filled with Fixed Advance Deposit Amount
  generateAdvanceUpiQr(upiId, advanceDeposit);
}

async function loadFloors() {
  const floorSelect = $('select-floor');
  floorSelect.innerHTML = '<option value="">Loading floors…</option>';

  try {
    let query = sb.from('floors').select('id, floor_number, building_id');
    const { data } = await query;
    floorsList = data || [];

    if (floorsList.length === 0) {
      floorSelect.innerHTML = '<option value="default_floor">Ground Floor (Main)</option>';
    } else {
      floorSelect.innerHTML = '<option value="">-- Choose Floor --</option>' +
        floorsList.map(f => `<option value="${f.id}">Floor ${f.floor_number}</option>`).join('');
    }
  } catch {
    floorSelect.innerHTML = '<option value="default_floor">Ground Floor (Main)</option>';
  }
}

async function onFloorSelected() {
  selectedFloorId = $('select-floor').value;
  const roomSelect = $('select-room');
  const bedSelect = $('select-bed');
  bedSelect.innerHTML = '<option value="">Select room first</option>';
  bedSelect.disabled = true;

  if (!selectedFloorId) {
    roomSelect.innerHTML = '<option value="">Select floor first</option>';
    roomSelect.disabled = true;
    return;
  }

  roomSelect.disabled = false;
  roomSelect.innerHTML = '<option value="">Loading rooms…</option>';

  try {
    const { data } = await sb
      .from('rooms')
      .select('id, room_number, capacity, room_type')
      .eq('floor_id', selectedFloorId);

    roomsList = data || [];
    if (roomsList.length === 0) {
      roomSelect.innerHTML = '<option value="default_room">Room 101 (Standard)</option>';
    } else {
      roomSelect.innerHTML = '<option value="">-- Choose Room --</option>' +
        roomsList.map(r => `<option value="${r.id}">Room ${r.room_number} (${r.room_type || 'Standard'})</option>`).join('');
    }
  } catch {
    roomSelect.innerHTML = '<option value="default_room">Room 101 (Standard)</option>';
  }
}

async function onRoomSelected() {
  selectedRoomId = $('select-room').value;
  const bedSelect = $('select-bed');

  if (!selectedRoomId) {
    bedSelect.innerHTML = '<option value="">Select room first</option>';
    bedSelect.disabled = true;
    return;
  }

  bedSelect.disabled = false;
  bedSelect.innerHTML = '<option value="">Loading available beds…</option>';

  try {
    const { data } = await sb
      .from('beds')
      .select('id, bed_number, status')
      .eq('room_id', selectedRoomId);

    bedsList = data || [];
    const availableBeds = bedsList.filter(b => b.status === 'Available' || !b.status);

    if (availableBeds.length === 0) {
      bedSelect.innerHTML = '<option value="default_bed">Bed A (Available)</option>';
    } else {
      bedSelect.innerHTML = '<option value="">-- Choose Available Bed --</option>' +
        availableBeds.map(b => `<option value="${b.id}">Bed ${b.bed_number}</option>`).join('');
    }
  } catch {
    bedSelect.innerHTML = '<option value="default_bed">Bed A (Available)</option>';
  }
}

function onBedSelected() {
  selectedBedId = $('select-bed').value;
}

// ── Generate Dynamic UPI QR Code for Fixed Advance Deposit ─────
function generateAdvanceUpiQr(upiId, amount) {
  const img = $('advance-upi-qr');
  if (!img) return;

  const cleanUpi = (upiId && upiId !== 'Not Configured' && upiId !== 'Pending Hostel Setup') ? upiId.trim() : '9390952712-3@ybl';
  const amountVal = (amount && amount > 0) ? amount : 100.00;
  const amountStr = amountVal.toFixed(2);
  const payeeName = encodeURIComponent(hostelData?.name || 'Hostel Management');

  // Standard UPI Intent URI encoding exact payment parameters
  const upiUri = `upi://pay?pa=${cleanUpi}&pn=${payeeName}&am=${amountStr}&cu=INR&tn=Advance%20Deposit%20Payment`;
  const encodedUri = encodeURIComponent(upiUri);

  // Set high-contrast QR image source via QR server API
  img.src = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodedUri}`;

  // Client-side fallback using QRCode library if available
  if (window.QRCode && window.QRCode.toDataURL) {
    window.QRCode.toDataURL(upiUri, { width: 250, margin: 1 }, (err, url) => {
      if (!err && url) {
        img.src = url;
      }
    });
  }
}

// ── Submit Payment Verification Payload ────────────────────────
async function submitPaymentVerification() {
  const fullName   = $('input-full-name')?.value.trim() || currentUser.user_metadata?.full_name || currentUser.email.split('@')[0];
  const phone      = $('input-phone')?.value.trim() || currentUser.user_metadata?.phone || null;
  const utr        = $('input-utr').value.trim();
  const joiningDate = $('input-joining-date').value;
  const aadhaar    = $('input-aadhaar').value.trim();
  const studentId  = $('input-student-id').value.trim();

  if (!fullName || !phone || !utr || !joiningDate || !aadhaar) {
    toast('Please fill in your Full Name, Phone Number, UTR, joining date, and Aadhaar card details', 'error');
    return;
  }

  const roomSelect = $('select-room');
  const bedSelect  = $('select-bed');
  let roomText   = roomSelect.options[roomSelect.selectedIndex]?.text || '';
  let bedText    = bedSelect.options[bedSelect.selectedIndex]?.text || '';

  if (!roomText || roomText.toLowerCase().includes('select') || roomText.toLowerCase().includes('choose') || roomText.toLowerCase().includes('loading')) {
    roomText = 'Room 101';
  }
  if (!bedText || bedText.toLowerCase().includes('select') || bedText.toLowerCase().includes('choose') || bedText.toLowerCase().includes('loading')) {
    bedText = 'Bed A';
  }

  const btn = $('btn-submit-booking');
  btn.disabled = true;
  btn.textContent = 'Submitting Verification…';

  const newId = (typeof crypto !== 'undefined' && crypto.randomUUID)
    ? crypto.randomUUID()
    : ('reg_' + Date.now() + Math.random().toString(36).substring(2, 7));

  const advanceVal = paymentConfig?.advance_deposit ? parseFloat(paymentConfig.advance_deposit) : 0.00;

  const payload = {
    id: newId,
    hostel_id: hostelId || null,
    full_name: fullName,
    email: currentUser.email,
    phone: phone,
    student_id_number: studentId || null,
    aadhaar_number: aadhaar,
    room_number: roomText,
    bed_number: bedText,
    preferred_floor_id: selectedFloorId || null,
    preferred_room_id: selectedRoomId || null,
    preferred_bed_id: selectedBedId || null,
    notes: `UTR: ${utr} | Joining: ${joiningDate} | Advance: ₹${advanceVal}`,
    status: 'Pending'
  };

  try {
    const { error } = await sb
      .from('student_self_registrations')
      .insert(payload);

    if (error) throw error;

    currentRegistration = payload;
    toast('Payment submitted for verification!', 'success');
    renderUnderVerificationScreen();
  } catch (err) {
    console.error('Submission error:', err);
    toast(err?.message || 'Submission failed. Please check your connection and try again.', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '✅   Submit Payment & Bed Booking';
  }
}

let verificationPollTimer = null;

// ── Render Under Verification Screen ───────────────────────────
function renderUnderVerificationScreen() {
  showScreen('verification');
  
  if (currentRegistration) {
    let rText = currentRegistration.room_number || '';
    let bText = currentRegistration.bed_number || '';
    if (rText.toLowerCase().includes('select') || rText.toLowerCase().includes('choose') || rText.toLowerCase().includes('loading')) rText = '';
    if (bText.toLowerCase().includes('select') || bText.toLowerCase().includes('choose') || bText.toLowerCase().includes('loading')) bText = '';

    const bedInfo = [rText, bText].filter(Boolean).join(' · ');
    $('verif-bed-info').textContent = bedInfo || 'Requested Bed';
    
    // Extract UTR & Joining Date from notes if present
    const notes = currentRegistration.notes || '';
    const utrMatch = notes.match(/UTR:\s*([^\s|]+)/);
    const joinMatch = notes.match(/Joining:\s*([^\s|]+)/);

    $('verif-utr-no').textContent = utrMatch ? utrMatch[1] : 'Submitted';
    $('verif-joining-date').textContent = joinMatch ? joinMatch[1] : 'Pending Verification';

    const advanceVal = paymentConfig?.advance_deposit ? parseFloat(paymentConfig.advance_deposit) : 0.00;
    $('verif-advance-amount').textContent = `₹${advanceVal.toFixed(2)}`;
  }

  // Poll status every 3 seconds while on verification screen
  if (!verificationPollTimer) {
    verificationPollTimer = setInterval(async () => {
      const verifScreen = $('screen-verification');
      if (currentUser && verifScreen && verifScreen.classList.contains('active')) {
        const { data: regs } = await sb
          .from('student_self_registrations')
          .select('status')
          .eq('email', currentUser.email)
          .order('created_at', { ascending: false })
          .limit(1);

        if (regs && regs.length > 0 && regs[0].status === 'Approved') {
          clearInterval(verificationPollTimer);
          verificationPollTimer = null;
          toast('Your payment has been verified and approved!', 'success');
          await routeAuthenticatedStudent();
        }
      } else {
        clearInterval(verificationPollTimer);
        verificationPollTimer = null;
      }
    }, 3000);
  }
}

// ── Render Approved Student Dashboard (Simple & Non-Complex) ───
async function renderApprovedDashboard() {
  showScreen('approved');

  const name = currentRegistration?.full_name || currentUser.user_metadata?.full_name || currentUser.email;
  $('appr-student-name').textContent = `Welcome, ${name}`;
  $('appr-hostel-name').textContent  = hostelData?.name || 'Main Campus Hostel';

  // Allocated Bed & Room resolution
  let rText = currentRegistration?.room_number || '';
  let bText = currentRegistration?.bed_number || '';

  if (rText.toLowerCase().includes('select') || rText.toLowerCase().includes('choose') || rText.toLowerCase().includes('loading')) {
    rText = '';
  }
  if (bText.toLowerCase().includes('select') || bText.toLowerCase().includes('choose') || bText.toLowerCase().includes('loading')) {
    bText = '';
  }

  // Try fetching actual room and bed numbers if preferred IDs exist and current texts were placeholder/empty
  if ((!rText || !bText) && (currentRegistration?.preferred_room_id || currentRegistration?.preferred_bed_id)) {
    try {
      if (!rText && currentRegistration.preferred_room_id) {
        const { data: rm } = await sb.from('rooms').select('room_number').eq('id', currentRegistration.preferred_room_id).maybeSingle();
        if (rm && rm.room_number) rText = `Room ${rm.room_number}`;
      }
      if (!bText && currentRegistration.preferred_bed_id) {
        const { data: bd } = await sb.from('beds').select('bed_number').eq('id', currentRegistration.preferred_bed_id).maybeSingle();
        if (bd && bd.bed_number) bText = `Bed ${bd.bed_number}`;
      }
    } catch (e) {
      console.warn('Error fetching room/bed details:', e);
    }
  }

  const roomBed = [rText, bText].filter(Boolean).join(' · ');
  $('appr-room-bed').textContent = roomBed ? roomBed : 'Allocated Bed & Room Confirmed';

  const notes = currentRegistration?.notes || '';
  const joinMatch = notes.match(/Joining:\s*([^\s|]+)/);
  const joiningDateStr = joinMatch ? joinMatch[1] : new Date().toISOString().split('T')[0];
  $('appr-joining-schedule').textContent = `Joining Date: ${joiningDateStr}`;

  // 10-DAY MONTHLY DUE DATE COUNTDOWN REMINDER
  try {
    const today = new Date();
    const joinDateObj = new Date(joiningDateStr);
    const dayOfMonth = joinDateObj.getDate() || 8;

    let nextDueDate = new Date(today.getFullYear(), today.getMonth(), dayOfMonth);
    if (today.getDate() > dayOfMonth) {
      nextDueDate = new Date(today.getFullYear(), today.getMonth() + 1, dayOfMonth);
    }

    const diffTime = nextDueDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    const banner = $('appr-payment-due-banner');
    if (banner) {
      if (diffDays <= 10) {
        banner.style.display = 'block';
        const formattedDueDate = nextDueDate.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
        $('appr-due-heading').textContent = `⏰ Monthly Hostel Fee Due in ${diffDays <= 0 ? '0' : diffDays} Day${diffDays === 1 ? '' : 's'}!`;
        $('appr-due-text').textContent = `Your monthly fee payment is due on ${formattedDueDate}. Please scan the UPI QR below to pay and submit your UTR reference number.`;
      } else {
        banner.style.display = 'none';
      }
    }
  } catch (e) {
    console.warn('Error calculating due date:', e);
  }

  // Profile Details
  $('appr-email').textContent = currentUser.email;
  $('appr-phone').textContent = currentRegistration?.phone || currentUser.user_metadata?.phone || '—';
  $('appr-aadhaar').textContent = currentRegistration?.aadhaar_number || 'Verified';

  const monthlyFeeVal = paymentConfig?.monthly_fee ? parseFloat(paymentConfig.monthly_fee) : 0;
  const monthlyFeeStr = `₹${monthlyFeeVal.toFixed(2)}`;
  $('appr-monthly-fee').textContent = `${monthlyFeeStr} / month`;

  // Setup Monthly UPI QR
  const upiId = paymentConfig?.upi_id ? paymentConfig.upi_id : 'Pending Hostel Setup';
  $('appr-monthly-upi-id').textContent = upiId;

  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  const currentMonthStr = `${monthNames[new Date().getMonth()]} ${new Date().getFullYear()}`;
  if ($('input-billing-month')) $('input-billing-month').value = currentMonthStr;

  generateMonthlyUpiQr(upiId, monthlyFeeVal);

  // Load Hostel Announcements
  await loadAnnouncements();
}

function generateMonthlyUpiQr(upiId, amount) {
  const img = $('appr-monthly-qr');
  if (!img) return;

  const cleanUpi = (upiId && upiId !== 'Not Configured' && upiId !== 'Pending Hostel Setup') ? upiId.trim() : '9390952712-3@ybl';
  const amountVal = (amount && amount > 0) ? amount : 5000.00;
  const amountStr = amountVal.toFixed(2);
  const payeeName = encodeURIComponent(hostelData?.name || 'Hostel Management');

  const upiUri = `upi://pay?pa=${cleanUpi}&pn=${payeeName}&am=${amountStr}&cu=INR&tn=Monthly%20Hostel%20Fee`;
  const encodedUri = encodeURIComponent(upiUri);

  img.src = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodedUri}`;
}

// ── Submit Monthly Payment UTR ─────────────────────────────────
async function submitMonthlyPaymentUtr() {
  const utr          = $('input-monthly-utr').value.trim();
  const billingMonth = $('input-billing-month').value.trim();

  if (!utr || !billingMonth) {
    toast('Please enter both UTR number and Billing Month', 'error');
    return;
  }

  const btn = $('btn-submit-monthly');
  btn.disabled = true;
  btn.textContent = 'Submitting UTR…';

  const monthlyVal = paymentConfig?.monthly_fee ? parseFloat(paymentConfig.monthly_fee) : 0.00;

  const payload = {
    hostel_id: hostelId || null,
    student_id: currentRegistration?.id || null,
    email: currentUser.email,
    full_name: currentRegistration?.full_name || currentUser.user_metadata?.full_name || currentUser.email.split('@')[0],
    room_number: currentRegistration?.room_number || 'Room',
    bed_number: currentRegistration?.bed_number || 'Bed',
    amount: monthlyVal,
    utr_number: utr,
    billing_month: billingMonth,
    status: 'Pending'
  };

  try {
    const { error } = await sb
      .from('monthly_payment_submissions')
      .insert(payload);

    if (error) throw error;

    toast('Monthly payment UTR submitted for admin verification!', 'success');
    $('input-monthly-utr').value = '';
  } catch (err) {
    console.error('Monthly UTR submission error:', err);
    toast(err?.message || 'Submission failed. Please try again.', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '💸   Submit Monthly Fee UTR';
  }
}

// ── Load Hostel Announcements ──────────────────────────────────
async function loadAnnouncements() {
  const container = $('approved-announcements-list');
  if (!container) return;

  container.innerHTML = '<div class="empty-state">Loading announcements…</div>';

  try {
    const { data, error } = await sb
      .from('announcements')
      .select('id, title, content, created_at')
      .order('created_at', { ascending: false })
      .limit(5);

    if (error || !data || data.length === 0) {
      container.innerHTML = '<div class="empty-state">📢 No announcements posted yet.</div>';
      return;
    }

    container.innerHTML = data.map(a => `
      <div class="announcement-card">
        <div class="ann-title">${escapeHtml(a.title)}</div>
        <div class="ann-content">${escapeHtml(a.content)}</div>
        <div class="ann-date">${new Date(a.created_at).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</div>
      </div>
    `).join('');
  } catch {
    container.innerHTML = '<div class="empty-state">📢 No announcements posted yet.</div>';
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m]));
}
