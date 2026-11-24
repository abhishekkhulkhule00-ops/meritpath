import 'dotenv/config';
import crypto from 'node:crypto';
import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import Razorpay from 'razorpay';
import admin from 'firebase-admin';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

const PORT = Number(process.env.PORT || 8080);
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  'ai-studio-android-meritpat-2bce691d-4c89-4d71-a0e0-e05937575787';
const RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID || '';
const RAZORPAY_KEY_SECRET = process.env.RAZORPAY_KEY_SECRET || '';
const RAZORPAY_WEBHOOK_SECRET = process.env.RAZORPAY_WEBHOOK_SECRET || '';
const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || '')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

if (!admin.apps.length) {
  const inlineCreds = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (inlineCreds) {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(inlineCreds)),
      projectId: process.env.FIREBASE_PROJECT_ID || 'gen-lang-client-0126130709',
    });
  } else {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
      projectId: process.env.FIREBASE_PROJECT_ID || 'gen-lang-client-0126130709',
    });
  }
}

const db = getFirestore(admin.app(), DATABASE_ID);
const razorpay =
  RAZORPAY_KEY_ID && RAZORPAY_KEY_SECRET
    ? new Razorpay({ key_id: RAZORPAY_KEY_ID, key_secret: RAZORPAY_KEY_SECRET })
    : null;

interface AuthedRequest extends Request {
  uid?: string;
  email?: string;
  emailVerified?: boolean;
}

const app = express();
app.use(helmet());
app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin || ALLOWED_ORIGINS.length === 0 || ALLOWED_ORIGINS.includes(origin)) {
        callback(null, true);
      } else {
        callback(new Error('Origin not allowed by CORS'));
      }
    },
  })
);

// Razorpay webhook MUST receive raw body BEFORE express.json()
app.post(
  '/api/payments/webhook',
  express.raw({ type: 'application/json' }),
  async (req: Request, res: Response) => {
    try {
      if (!RAZORPAY_WEBHOOK_SECRET) {
        return res.status(503).json({ error: 'Webhook secret not configured.' });
      }
      const receivedSig = req.header('x-razorpay-signature') || '';
      const expectedSig = crypto
        .createHmac('sha256', RAZORPAY_WEBHOOK_SECRET)
        .update(req.body)
        .digest('hex');

      const sigValid =
        receivedSig.length === expectedSig.length &&
        crypto.timingSafeEqual(Buffer.from(receivedSig), Buffer.from(expectedSig));

      if (!sigValid) {
        return res.status(401).json({ error: 'Invalid webhook signature.' });
      }

      const event = JSON.parse(req.body.toString('utf8'));
      if (event.event === 'payment.captured') {
        const entity = event.payload?.payment?.entity;
        const orderId = String(entity?.order_id || '');
        const paymentId = String(entity?.id || '');
        const notes = entity?.notes || {};
        const uid = String(notes.uid || '');
        const packageId = String(notes.packageId || '');

        if (orderId && paymentId && uid && packageId) {
          await activateVerifiedPaymentAndPackage({
            uid,
            packageId,
            orderId,
            paymentId,
            signature: receivedSig,
            method: String(entity?.method || 'razorpay'),
            mode: RAZORPAY_KEY_ID.startsWith('rzp_live_') ? 'LIVE' : 'SANDBOX',
            promoCode: String(notes.promoCode || ''),
          });
        }
      }
      return res.json({ status: 'ok' });
    } catch (err) {
      console.error('Webhook processing error:', err);
      return res.status(500).json({ error: 'Webhook processing failed.' });
    }
  }
);

app.use(express.json({ limit: '1mb' }));

async function requireFirebaseAuth(req: AuthedRequest, res: Response, next: NextFunction) {
  try {
    const authHeader = req.header('Authorization') || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    if (!token) {
      return res.status(401).json({ error: 'Missing Authorization bearer token.' });
    }
    const decoded = await admin.auth().verifyIdToken(token);
    req.uid = decoded.uid;
    req.email = decoded.email || '';
    req.emailVerified = Boolean(decoded.email_verified);
    return next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired Firebase ID token.' });
  }
}

function computeServerPrice(
  pkgData: FirebaseFirestore.DocumentData,
  promoData?: FirebaseFirestore.DocumentData
): { originalAmount: number; discountAmount: number; finalAmount: number } {
  const original = Math.max(0, Number(pkgData.originalPrice || 0));
  const pkgType = String(pkgData.discountType || 'NONE');
  const pkgVal = Math.max(0, Number(pkgData.discountValue || 0));
  let pkgDiscount = 0;
  if (pkgType === 'FIXED') pkgDiscount = pkgVal;
  if (pkgType === 'PERCENT') pkgDiscount = Math.round((original * Math.min(100, pkgVal)) / 100);
  pkgDiscount = Math.min(original, pkgDiscount);

  let afterPkg = Math.max(0, original - pkgDiscount);
  let promoDiscount = 0;
  const now = Date.now();
  if (
    promoData &&
    promoData.active === true &&
    (!promoData.expiryEpochMs || Number(promoData.expiryEpochMs) >= now) &&
    Number(promoData.usedCount || 0) < Number(promoData.maxUsage || 1) &&
    afterPkg >= Number(promoData.minOrderAmount || 0)
  ) {
    const pType = String(promoData.discountType || 'NONE');
    const pVal = Math.max(0, Number(promoData.discountValue || 0));
    if (pType === 'FIXED') promoDiscount = pVal;
    if (pType === 'PERCENT') promoDiscount = Math.round((afterPkg * Math.min(100, pVal)) / 100);
    promoDiscount = Math.min(afterPkg, promoDiscount);
  }

  const finalAmount = Math.max(0, afterPkg - promoDiscount);
  return {
    originalAmount: original,
    discountAmount: original - finalAmount,
    finalAmount,
  };
}

async function activateVerifiedPaymentAndPackage(params: {
  uid: string;
  packageId: string;
  orderId: string;
  paymentId: string;
  signature: string;
  method: string;
  mode: 'SANDBOX' | 'LIVE';
  promoCode?: string;
}): Promise<{ paymentDocId: string; receiptNumber: string; finalAmount: number }> {
  const pkgSnap = await db.collection('packages').doc(params.packageId).get();
  if (!pkgSnap.exists || pkgSnap.data()?.active !== true) {
    throw new Error('Selected package is invalid or inactive.');
  }
  const pkg = pkgSnap.data()!;
  const userSnap = await db.collection('users').doc(params.uid).get();
  const user = userSnap.data() || {};

  let promoData: FirebaseFirestore.DocumentData | undefined;
  const promoClean = (params.promoCode || '').trim().toLowerCase();
  if (promoClean) {
    const promoSnap = await db.collection('promo_codes').doc(`promo_${promoClean}`).get();
    if (promoSnap.exists) promoData = promoSnap.data();
  }

  const pricing = computeServerPrice(pkg, promoData);
  const payDocId = `pay_${params.orderId.replace(/[^a-zA-Z0-9_-]/g, '').slice(-20)}`;
  const receiptNumber = `MP-RCPT-${Date.now().toString().slice(-6)}`;
  const nowMs = Date.now();
  const validityDays = Math.max(1, Number(pkg.validityDays || 180));
  const expiresAtEpochMs = nowMs + validityDays * 24 * 60 * 60 * 1000;

  await db.runTransaction(async (tx) => {
    const payRef = db.collection('payments').document(payDocId);
    const existingPay = await tx.get(payRef);
    if (existingPay.exists && existingPay.data()?.status === 'SUCCESS') {
      return; // Idempotent replay guard
    }

    tx.set(payRef, {
      userId: params.uid,
      studentName: String(user.fullName || 'Student').slice(0, 120),
      studentEmail: String(user.email || '').slice(0, 200),
      packageId: params.packageId,
      packageName: String(pkg.name || 'Counselling Package').slice(0, 120),
      packageTier: String(pkg.tier || 'BASIC'),
      originalAmount: pricing.originalAmount,
      discountAmount: pricing.discountAmount,
      promoCode: promoClean ? promoClean.toUpperCase() : '',
      finalAmount: pricing.finalAmount,
      currency: 'INR',
      gatewayOrderId: params.orderId,
      gatewayPaymentId: params.paymentId,
      gatewaySignature: params.signature,
      paymentMethod: params.method.slice(0, 60),
      mode: params.mode,
      status: 'SUCCESS',
      receiptNumber,
      verifiedByBackend: true,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    const entRef = db.collection('student_packages').document(`${params.uid}_${params.packageId}`);
    tx.set(entRef, {
      userId: params.uid,
      packageId: params.packageId,
      packageName: String(pkg.name || 'Counselling Package').slice(0, 120),
      tier: String(pkg.tier || 'BASIC'),
      paymentId: payDocId,
      features: Array.isArray(pkg.features) ? pkg.features.slice(0, 20) : [],
      active: true,
      activatedAtEpochMs: nowMs,
      expiresAtEpochMs,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (promoClean) {
      const promoRef = db.collection('promo_codes').doc(`promo_${promoClean}`);
      tx.update(promoRef, {
        usedCount: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  });

  return { paymentDocId: payDocId, receiptNumber, finalAmount: pricing.finalAmount };
}

app.get('/health', (_req, res) => {
  res.json({
    ok: true,
    service: 'meritpath-backend',
    mode: RAZORPAY_KEY_ID.startsWith('rzp_live_') ? 'LIVE' : 'SANDBOX',
    razorpayConfigured: Boolean(razorpay),
  });
});

app.post('/api/payments/create-order', requireFirebaseAuth, async (req: AuthedRequest, res: Response) => {
  try {
    if (!razorpay) {
      return res.status(503).json({ error: 'Razorpay credentials not configured on backend.' });
    }
    const packageId = String(req.body?.packageId || '').trim();
    const promoCode = String(req.body?.promoCode || '').trim();
    if (!/^[a-zA-Z0-9_-]{1,128}$/.test(packageId)) {
      return res.status(400).json({ error: 'Invalid packageId.' });
    }

    const pkgSnap = await db.collection('packages').doc(packageId).get();
    if (!pkgSnap.exists || pkgSnap.data()?.active !== true) {
      return res.status(404).json({ error: 'Package not found or inactive.' });
    }

    let promoData: FirebaseFirestore.DocumentData | undefined;
    if (promoCode) {
      const promoSnap = await db.collection('promo_codes').doc(`promo_${promoCode.toLowerCase()}`).get();
      if (promoSnap.exists) promoData = promoSnap.data();
    }

    const pricing = computeServerPrice(pkgSnap.data()!, promoData);
    const amountPaise = pricing.finalAmount * 100;
    const order = await razorpay.orders.create({
      amount: amountPaise,
      currency: 'INR',
      receipt: `mp_${req.uid!.slice(0, 8)}_${Date.now().toString().slice(-6)}`,
      notes: {
        uid: req.uid!,
        packageId,
        promoCode: promoCode.toUpperCase(),
      },
    });

    return res.json({
      orderId: order.id,
      amountPaise,
      finalAmountInr: pricing.finalAmount,
      currency: 'INR',
      keyId: RAZORPAY_KEY_ID,
    });
  } catch (err) {
    console.error('create-order error:', err);
    return res.status(500).json({ error: 'Unable to create Razorpay order.' });
  }
});

app.post('/api/payments/verify-signature', requireFirebaseAuth, async (req: AuthedRequest, res: Response) => {
  try {
    if (!RAZORPAY_KEY_SECRET) {
      return res.status(503).json({ error: 'Razorpay key secret not configured.' });
    }
    const orderId = String(req.body?.razorpay_order_id || '').trim();
    const paymentId = String(req.body?.razorpay_payment_id || '').trim();
    const signature = String(req.body?.razorpay_signature || '').trim();
    const packageId = String(req.body?.packageId || '').trim();
    const promoCode = String(req.body?.promoCode || '').trim();
    const method = String(req.body?.method || 'upi').trim();

    if (!orderId || !paymentId || !signature || !packageId) {
      return res.status(400).json({ error: 'Missing required Razorpay verification fields.' });
    }

    const expected = crypto
      .createHmac('sha256', RAZORPAY_KEY_SECRET)
      .update(`${orderId}|${paymentId}`)
      .digest('hex');

    const valid =
      signature.length === expected.length &&
      crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expected));

    if (!valid) {
      return res.status(400).json({ verified: false, error: 'Signature verification failed.' });
    }

    const result = await activateVerifiedPaymentAndPackage({
      uid: req.uid!,
      packageId,
      orderId,
      paymentId,
      signature,
      method,
      mode: RAZORPAY_KEY_ID.startsWith('rzp_live_') ? 'LIVE' : 'SANDBOX',
      promoCode,
    });

    return res.json({ verified: true, ...result });
  } catch (err) {
    console.error('verify-signature error:', err);
    return res.status(500).json({ error: 'Payment verification failed.' });
  }
});

app.listen(PORT, () => {
  console.log(`MeritPath verification backend listening on port ${PORT}`);
});
