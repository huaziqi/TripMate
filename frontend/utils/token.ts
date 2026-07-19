export function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(
      decodeBase64Url(token.split('.')[1])
    )
    if (!payload.exp) return true
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

function decodeBase64Url(base64url: string): string {
  let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/')
  const pad = base64.length % 4
  if (pad) base64 += '='.repeat(4 - pad)
  return atob(base64)
}

export function clearExpiredToken(): void {
  uni.removeStorageSync('token')
}
