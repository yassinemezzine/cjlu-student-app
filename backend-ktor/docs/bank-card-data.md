# Bank card and payment-sensitive data

## MVP (this project)

- The app and Ktor backend implement a **campus student workflow** stack; they are not a PCI DSS–certified cardholder environment.
- **Policy**: students must not paste full primary account numbers (PAN) into free-text fields. The server rejects payloads that look like 13–19 contiguous digits in `contactInfo`, `notes`, `PATCH` profile text, and similar (see `PanValidation` in the backend).
- **Attachments** for bank-related services are stored as files under `uploads/` on the server filesystem. That is convenient for development but is **not** equivalent to encrypted, access-controlled cardholder storage.
- **Scope**: anything beyond basic pattern blocking (CVV, magnetic stripe, PIN block, etc.) is explicitly out of scope for the MVP.

## Production / PCI-minded approach

- **Never** send raw PAN, CVV/CVC, or magnetic-stripe data through your own JSON fields.
- Use a **PCI DSS–validated** channel to collect or tokenize cards: for example **Stripe Elements**, **Adyen Drop-in**, or a **bank-hosted** iframe or mobile SDK where card data posts directly to the payment provider.
- Your backend should store only **non-sensitive** references: customer id, last4, brand, token id, mandate references—whatever the PSP documents as safe for your tier.
- Add explicit **data retention, encryption at rest, access control, and audit** policies if you ever store bank details beyond tokens.

This document is guidance only and does not constitute legal or compliance advice.
