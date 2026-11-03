// apps/web/src/v3/components/ui/Button.tsx
// متوافق مع المستودع: لا tailwind ولا framer-motion ولا clsx — أنماط v3.css المضافة.
import type { ButtonHTMLAttributes } from 'react'

type ClassValue = string | number | false | null | undefined | Record<string, boolean>

/** بديل محلي صغير عن `clsx` + `tailwind-merge` بدون أي تبعية جديدة. */
export function cn(...inputs: ClassValue[]): string {
  const out: string[] = []
  for (const input of inputs) {
    if (!input && input !== 0) continue
    if (typeof input === 'string' || typeof input === 'number') {
      out.push(String(input))
      continue
    }
    for (const [key, enabled] of Object.entries(input)) {
      if (enabled) out.push(key)
    }
  }
  return out.join(' ')
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'default' | 'outline' | 'ghost' | 'destructive'
  size?: 'sm' | 'md' | 'lg'
}

export function Button({ className, variant = 'default', size = 'md', type = 'button', ...props }: ButtonProps) {
  return <button type={type} className={cn('v3-btn', `v3-btn-${variant}`, `v3-btn-${size}`, className)} {...props} />
}
