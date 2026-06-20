import { useRef } from 'react'
import { ImagePlus, X } from 'lucide-react'

interface Props {
  files: File[]
  onChange: (files: File[]) => void
  disabled?: boolean
}

export default function PhotoUpload({ files, onChange, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)

  function handleSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(e.target.files || [])
    onChange([...files, ...selected].slice(0, 8))
    e.target.value = ''
  }

  function remove(i: number) {
    onChange(files.filter((_, idx) => idx !== i))
  }

  return (
    <div>
      <div className="flex flex-wrap gap-2">
        {files.map((file, i) => (
          <div
            key={i}
            className="relative w-[76px] h-[76px] rounded-2xl overflow-hidden fade-up"
            style={{ animationDelay: `${i * 40}ms`, border: '1px solid var(--border)' }}
          >
            <img
              src={URL.createObjectURL(file)}
              alt=""
              className="w-full h-full object-cover"
            />
            {!disabled && (
              <button
                type="button"
                onClick={() => remove(i)}
                className="absolute top-1 right-1 w-5 h-5 rounded-full flex items-center justify-center cursor-pointer transition-transform active:scale-90"
                style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}
                aria-label="Rasmni o'chirish"
              >
                <X size={11} color="#fff" strokeWidth={3} />
              </button>
            )}
          </div>
        ))}

        {files.length < 8 && !disabled && (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="w-[76px] h-[76px] rounded-2xl flex flex-col items-center justify-center gap-1 cursor-pointer transition-all duration-200 active:scale-95"
            style={{
              border: '1.5px dashed rgba(99,102,241,0.35)',
              background: 'rgba(99,102,241,0.06)',
              color: 'var(--accent)',
            }}
            aria-label="Rasm qo'shish"
          >
            <ImagePlus size={20} />
            <span style={{ fontSize: 11, fontWeight: 500 }}>Rasm</span>
          </button>
        )}
      </div>

      {files.length === 0 && (
        <p className="text-xs mt-2" style={{ color: 'var(--fg-muted)' }}>
          Kamida 1 ta rasm (max 8 ta)
        </p>
      )}

      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleSelect}
      />
    </div>
  )
}
