function pickFirstNumber(...values) {
  for (const value of values) {
    if (typeof value === 'number' && !Number.isNaN(value)) {
      return value
    }
  }
  return 0
}

export default function normalizeWheel(event) {
  const pixelX = pickFirstNumber(event.deltaX, event.wheelDeltaX && -event.wheelDeltaX)
  const pixelY = pickFirstNumber(
    event.deltaY,
    event.wheelDeltaY && -event.wheelDeltaY,
    event.wheelDelta && -event.wheelDelta,
    event.detail
  )

  return {
    spinX: pixelX === 0 ? 0 : pixelX > 0 ? 1 : -1,
    spinY: pixelY === 0 ? 0 : pixelY > 0 ? 1 : -1,
    pixelX,
    pixelY
  }
}
