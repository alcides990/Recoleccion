-- Conserva una fotografía de la deuda existente al iniciar cada suspensión.
ALTER TABLE historial_suspensiones_servicio
    ADD COLUMN cantidad_periodos_pendientes INT NOT NULL DEFAULT 0 AFTER fecha_hasta;

