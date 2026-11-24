import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut, User } from 'firebase/auth';
import {
  collection,
  doc,
  getDoc,
  getFirestore,
  onSnapshot,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
};

const DB_ID =
  import.meta.env.VITE_FIRESTORE_DATABASE_ID ||
  'ai-studio-android-meritpat-2bce691d-4c89-4d71-a0e0-e05937575787';

const BOOTSTRAPPED_ADMIN_EMAIL = 'abhishekkhulkhule@gmail.com';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app, DB_ID);

type AdminModule =
  | 'dashboard'
  | 'students'
  | 'courses'
  | 'colleges'
  | 'packages'
  | 'promos'
  | 'payments'
  | 'preferences'
  | 'counselling'
  | 'notifications'
  | 'reports'
  | 'settings';

const MODULE_LABELS: Record<AdminModule, string> = {
  dashboard: 'Dashboard & Analytics',
  students: 'Student Management',
  courses: 'Course Management',
  colleges: 'College & Cutoffs',
  packages: 'Packages & Pricing',
  promos: 'Promo & Combos',
  payments: 'Payments & Refunds',
  preferences: 'Preference Lists (Private)',
  counselling: 'Counselling Appointments',
  notifications: 'Notifications Center',
  reports: 'CSV Reports Export',
  settings: 'RBAC Staff & Settings',
};

function downloadCsv(filename: string, headers: string[], rows: (string | number)[][]) {
  const escapeCell = (val: string | number) => `"${String(val).replace(/"/g, '""')}"`;
  const csv = [headers.map(escapeCell).join(','), ...rows.map((r) => r.map(escapeCell).join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function AdminApp() {
  const [user, setUser] = useState<User | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [module, setModule] = useState<AdminModule>('dashboard');
  const [students, setStudents] = useState<any[]>([]);
  const [courses, setCourses] = useState<any[]>([]);
  const [colleges, setColleges] = useState<any[]>([]);
  const [packages, setPackages] = useState<any[]>([]);
  const [promos, setPromos] = useState<any[]>([]);
  const [payments, setPayments] = useState<any[]>([]);
  const [preferences, setPreferences] = useState<any[]>([]);
  const [appointments, setAppointments] = useState<any[]>([]);
  const [staff, setStaff] = useState<any[]>([]);
  const [statusMsg, setStatusMsg] = useState('');

  useEffect(() => {
    return onAuthStateChanged(auth, async (u) => {
      setUser(u);
      if (!u) {
        setRole(null);
        return;
      }
      if (u.emailVerified && u.email?.toLowerCase() === BOOTSTRAPPED_ADMIN_EMAIL) {
        setRole('SUPER_ADMIN');
        return;
      }
      const snap = await getDoc(doc(db, 'admins', u.uid));
      if (snap.exists() && snap.data()?.active === true) {
        setRole(String(snap.data()?.role || 'COUNSELLOR'));
      } else {
        setRole(null);
      }
    });
  }, []);

  useEffect(() => {
    if (!user || !role) return;
    const unsubs = [
      onSnapshot(collection(db, 'users'), (s) => setStudents(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'courses'), (s) => setCourses(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'colleges'), (s) => setColleges(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'packages'), (s) => setPackages(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'promo_codes'), (s) => setPromos(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'payments'), (s) => setPayments(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'student_preferences'), (s) => setPreferences(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'counselling_appointments'), (s) => setAppointments(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
      onSnapshot(collection(db, 'admins'), (s) => setStaff(s.docs.map((d) => ({ id: d.id, ...d.data() })))),
    ];
    return () => unsubs.forEach((u) => u());
  }, [user, role]);

  const totalRevenue = useMemo(
    () => payments.filter((p) => p.status === 'SUCCESS').reduce((sum, p) => sum + Number(p.finalAmount || 0), 0),
    [payments]
  );

  if (!user) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24 }}>
        <div style={{ maxWidth: 420, background: '#fff', padding: 32, borderRadius: 16, boxShadow: '0 8px 30px rgba(15,23,42,0.08)' }}>
          <h1 style={{ marginTop: 0, color: '#0D47A1' }}>MeritPath Admin Portal</h1>
          <p style={{ color: '#475569', lineHeight: 1.5 }}>
            Sign in with an authorized Google staff account to manage courses, colleges, packages, verified payments, and private student preference lists.
          </p>
          <button
            onClick={() => signInWithPopup(auth, new GoogleAuthProvider())}
            style={{ width: '100%', padding: '12px 16px', background: '#0D47A1', color: '#fff', border: 0, borderRadius: 10, fontWeight: 600, cursor: 'pointer' }}
          >
            Sign in with Google
          </button>
        </div>
      </div>
    );
  }

  if (!role) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24 }}>
        <div style={{ maxWidth: 440, background: '#fff', padding: 32, borderRadius: 16 }}>
          <h2>Unauthorized Account</h2>
          <p>Signed in as {user.email}. This account does not hold an active Admin or Counsellor role.</p>
          <button onClick={() => signOut(auth)} style={{ padding: '10px 16px', borderRadius: 8, border: '1px solid #CBD5E1' }}>
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <aside style={{ width: 260, background: '#0F172A', color: '#F8FAFC', padding: 20, display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 20, fontWeight: 700 }}>MeritPath</div>
          <div style={{ fontSize: 12, color: '#94A3B8' }}>Role: {role}</div>
        </div>
        {(Object.keys(MODULE_LABELS) as AdminModule[]).map((key) => (
          <button
            key={key}
            onClick={() => setModule(key)}
            style={{
              textAlign: 'left',
              padding: '10px 12px',
              borderRadius: 8,
              border: 0,
              cursor: 'pointer',
              background: module === key ? '#0D47A1' : 'transparent',
              color: module === key ? '#fff' : '#CBD5E1',
              fontWeight: module === key ? 600 : 400,
            }}
          >
            {MODULE_LABELS[key]}
          </button>
        ))}
        <button
          onClick={() => signOut(auth)}
          style={{ marginTop: 'auto', padding: '10px 12px', borderRadius: 8, background: '#1E293B', color: '#F87171', border: 0, cursor: 'pointer' }}
        >
          Sign Out ({user.email})
        </button>
      </aside>

      <main style={{ flex: 1, padding: 28, overflowY: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>{MODULE_LABELS[module]}</h1>
          {statusMsg && <span style={{ background: '#DCFCE7', color: '#166534', padding: '6px 12px', borderRadius: 8, fontSize: 13 }}>{statusMsg}</span>}
        </div>

        {module === 'dashboard' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 16 }}>
            <MetricCard title="Students" value={students.length} />
            <MetricCard title="Active Colleges" value={colleges.length} />
            <MetricCard title="Submitted Preferences" value={preferences.filter((p) => p.status !== 'DRAFT').length} />
            <MetricCard title="Verified Revenue" value={`₹${totalRevenue.toLocaleString('en-IN')}`} />
            <MetricCard title="Counselling Requests" value={appointments.length} />
            <MetricCard title="Active Packages" value={packages.length} />
          </div>
        )}

        {module === 'students' && (
          <SimpleTable
            headers={['Student ID', 'Name', 'Course', 'Percentile', 'Status', 'Action']}
            rows={students.map((s) => [
              s.studentCode || s.id,
              s.fullName,
              s.preferredCourseName || '-',
              s.percentile ?? 0,
              s.blocked ? 'BLOCKED' : s.counsellingStatus,
              <button
                key={s.id}
                onClick={async () => {
                  await updateDoc(doc(db, 'users', s.id), { blocked: !s.blocked, updatedAt: serverTimestamp() });
                  setStatusMsg(`Updated block state for ${s.fullName}`);
                }}
              >
                {s.blocked ? 'Unblock' : 'Block'}
              </button>,
            ])}
          />
        )}

        {module === 'preferences' && (
          <SimpleTable
            headers={['Student', 'Course', 'Percentile', 'Choices', 'Status', 'Counsellor Action']}
            rows={preferences.map((p) => [
              `${p.studentName} (${p.studentCode})`,
              p.courseName,
              p.percentile ?? 0,
              Array.isArray(p.items) ? p.items.length : 0,
              p.status,
              <div key={p.id} style={{ display: 'flex', gap: 8 }}>
                <button
                  onClick={async () => {
                    await updateDoc(doc(db, 'student_preferences', p.id), { status: 'REVIEWED', updatedAt: serverTimestamp() });
                    setStatusMsg(`Marked ${p.studentName}'s list REVIEWED`);
                  }}
                >
                  Mark Reviewed
                </button>
                <button
                  onClick={async () => {
                    await updateDoc(doc(db, 'student_preferences', p.id), { status: 'DRAFT', updatedAt: serverTimestamp() });
                    setStatusMsg(`Unlocked ${p.studentName}'s list to DRAFT`);
                  }}
                >
                  Unlock Draft
                </button>
              </div>,
            ])}
          />
        )}

        {module === 'payments' && (
          <SimpleTable
            headers={['Receipt', 'Student', 'Package', 'Final Amount', 'Mode', 'Status', 'Action']}
            rows={payments.map((p) => [
              p.receiptNumber || p.gatewayOrderId,
              p.studentName,
              p.packageName,
              `₹${p.finalAmount}`,
              p.mode,
              p.status,
              p.status === 'SUCCESS' ? (
                <button
                  key={p.id}
                  onClick={async () => {
                    await updateDoc(doc(db, 'payments', p.id), {
                      status: 'REFUNDED',
                      failureReason: 'Refunded from Admin Web Panel',
                      updatedAt: serverTimestamp(),
                    });
                    setStatusMsg(`Refund recorded for ${p.receiptNumber || p.id}`);
                  }}
                >
                  Mark Refunded
                </button>
              ) : (
                '-'
              ),
            ])}
          />
        )}

        {module === 'reports' && (
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <button
              onClick={() =>
                downloadCsv(
                  'meritpath-students.csv',
                  ['StudentCode', 'Name', 'Email', 'Course', 'Percentile', 'Category', 'Status'],
                  students.map((s) => [s.studentCode, s.fullName, s.email, s.preferredCourseName, s.percentile, s.category, s.counsellingStatus])
                )
              }
            >
              Export Students CSV
            </button>
            <button
              onClick={() =>
                downloadCsv(
                  'meritpath-payments.csv',
                  ['Receipt', 'Student', 'Package', 'AmountINR', 'Mode', 'Status'],
                  payments.map((p) => [p.receiptNumber, p.studentName, p.packageName, p.finalAmount, p.mode, p.status])
                )
              }
            >
              Export Payments CSV
            </button>
            <button
              onClick={() =>
                downloadCsv(
                  'meritpath-preferences.csv',
                  ['StudentCode', 'StudentName', 'Course', 'ChoicesCount', 'Status'],
                  preferences.map((p) => [p.studentCode, p.studentName, p.courseName, p.items?.length || 0, p.status])
                )
              }
            >
              Export Preference Lists CSV
            </button>
          </div>
        )}

        {module === 'notifications' && (
          <NotificationSender
            actorUid={user.uid}
            onSent={(msg) => setStatusMsg(msg)}
          />
        )}

        {!['dashboard', 'students', 'preferences', 'payments', 'reports', 'notifications'].includes(module) && (
          <div style={{ background: '#fff', padding: 20, borderRadius: 12 }}>
            <p>
              Showing <strong>{MODULE_LABELS[module]}</strong> live records synchronized with Firestore database <code>{DB_ID}</code>:
            </p>
            <ul>
              <li>Courses: {courses.length}</li>
              <li>Colleges: {colleges.length}</li>
              <li>Packages: {packages.length}</li>
              <li>Promo Codes: {promos.length}</li>
              <li>Appointments: {appointments.length}</li>
              <li>Staff Roles: {staff.length}</li>
            </ul>
          </div>
        )}
      </main>
    </div>
  );
}

function MetricCard({ title, value }: { title: string; value: string | number }) {
  return (
    <div style={{ background: '#fff', padding: 18, borderRadius: 12, border: '1px solid #E2E8F0' }}>
      <div style={{ fontSize: 12, color: '#64748B', fontWeight: 600 }}>{title}</div>
      <div style={{ fontSize: 26, fontWeight: 700, marginTop: 6 }}>{value}</div>
    </div>
  );
}

function SimpleTable({ headers, rows }: { headers: string[]; rows: React.ReactNode[][] }) {
  return (
    <div style={{ background: '#fff', borderRadius: 12, border: '1px solid #E2E8F0', overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
        <thead>
          <tr style={{ background: '#F1F5F9' }}>
            {headers.map((h) => (
              <th key={h} style={{ padding: 12, fontSize: 13 }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} style={{ borderTop: '1px solid #E2E8F0' }}>
              {r.map((cell, j) => (
                <td key={j} style={{ padding: 12, fontSize: 14 }}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function NotificationSender({ actorUid, onSent }: { actorUid: string; onSent: (msg: string) => void }) {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [type, setType] = useState('CAP_ROUND');

  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (!title.trim() || !message.trim()) return;
        const id = `notif_${Date.now()}`;
        await setDoc(doc(db, 'notifications', id), {
          title: title.trim(),
          message: message.trim(),
          type,
          targetUserId: 'ALL',
          audience: 'ALL',
          createdByUid: actorUid,
          createdAt: serverTimestamp(),
        });
        setTitle('');
        setMessage('');
        onSent('Broadcast notification published to all students.');
      }}
      style={{ background: '#fff', padding: 20, borderRadius: 12, maxWidth: 520, display: 'grid', gap: 12 }}
    >
      <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Notification Title" required style={{ padding: 10 }} />
      <select value={type} onChange={(e) => setType(e.target.value)} style={{ padding: 10 }}>
        <option value="CAP_ROUND">CAP Round</option>
        <option value="CUTOFF">Cutoff Update</option>
        <option value="DEADLINE">Deadline</option>
        <option value="ADMISSION">Admission Update</option>
      </select>
      <textarea value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Announcement message..." rows={4} required style={{ padding: 10 }} />
      <button type="submit" style={{ padding: 12, background: '#0D47A1', color: '#fff', border: 0, borderRadius: 8, cursor: 'pointer' }}>
        Broadcast Alert
      </button>
    </form>
  );
}

const rootEl = document.getElementById('root');
if (rootEl) {
  createRoot(rootEl).render(<AdminApp />);
}
