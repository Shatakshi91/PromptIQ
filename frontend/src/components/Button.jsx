import { Loader2 } from 'lucide-react'

export default function Button({
  children,
  loading = false,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
  disabled,
  icon: Icon,
  ...props
}) {
  const base =
    'inline-flex items-center justify-center font-medium transition-colors duration-150 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer rounded-lg'

  const variants = {
    primary:
      'bg-gray-900 hover:bg-gray-800 active:bg-gray-950 text-white',
    secondary:
      'bg-white hover:bg-gray-50 text-gray-700 border border-gray-200',
    outline:
      'bg-transparent hover:bg-gray-100 text-gray-700 border border-gray-300',
    ghost:
      'bg-transparent hover:bg-gray-100 text-gray-600 hover:text-gray-900',
    danger:
      'bg-red-600 hover:bg-red-700 text-white',
  }

  const sizes = {
    sm: 'text-xs px-3 py-1.5 gap-1.5',
    md: 'text-sm px-3.5 py-2 gap-2',
    lg: 'text-sm px-4 py-2.5 gap-2',
  }

  return (
    <button
      disabled={disabled || loading}
      className={`${base} ${variants[variant] ?? variants.primary} ${sizes[size] ?? sizes.md} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...props}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : Icon ? (
        <Icon className="w-4 h-4 shrink-0" />
      ) : null}
      <span>{children}</span>
    </button>
  )
}