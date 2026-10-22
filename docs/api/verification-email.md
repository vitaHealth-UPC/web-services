# Correo de verificación

El código de verificación de Tata contiene seis dígitos y vence a los 15 minutos. El adaptador de Resend utiliza `POST https://api.resend.com/emails`, autorización Bearer y contenido de texto en español.

## Configuración del proveedor

- `TATA_RESEND_API_KEY`: clave de API de Resend.
- `TATA_RESEND_SENDER`: remitente autorizado, por ejemplo `Tata <cuentas@dominio-verificado.com>`.

La cola requiere `tata.email.encryption.active-key-id` y un mapa `tata.email.encryption.keys` de identificadores a claves AES de 32 bytes codificadas en Base64. Estas propiedades se proporcionan mediante configuración externa de Spring. El arranque rechaza una clave activa ausente o inválida. Las claves anteriores se conservan en el mapa hasta procesar o cancelar sus solicitudes pendientes.

El cliente establece un límite de conexión de 5 segundos y de lectura de 10 segundos. La configuración ausente, un error HTTP, una respuesta sin identificador y una solicitud vencida producen un fallo de entrega. El identificador devuelto representa aceptación por el proveedor; no demuestra entrega al buzón.

## Reintentos y datos sensibles

La cabecera `Idempotency-Key` utiliza el identificador estable de la solicitud. El destinatario, código, asunto y vencimiento permanecen iguales al reintentar. Resend conserva la clave durante 24 horas; Tata limita los intentos a la vigencia del código.

El cifrado del payload usa AES-256-GCM con nonce aleatorio y autentica el identificador de solicitud y la versión de clave. La configuración contiene la clave activa y las anteriores necesarias durante una rotación. No se generan claves efímeras ni se incluyen claves predeterminadas de producción. Los mensajes de error del adaptador no conservan cuerpos del proveedor, destinatarios ni códigos.

## Alcance de implementación

Registro y reenvío persisten una solicitud cifrada en la misma transacción que su cuenta. Un rollback revierte también la solicitud. El dispatcher procesa lotes de hasta 20 solicitudes con reservas de 60 segundos y llama al proveedor fuera de las transacciones de base de datos. Una caída permite recuperar la reserva vencida. Los fallos se reintentan con demora creciente, hasta 5 minutos entre intentos, conservando el identificador de idempotencia.

Antes de reservar, el dispatcher comprueba la cuenta pendiente, el hash del código vigente y su vencimiento. Cancela solicitudes vencidas, reemplazadas o de cuentas ausentes. Aceptación y cancelación eliminan el código cifrado. El token de reserva impide que un worker antiguo sobrescriba el estado de una nueva reserva. Una renovación posterior a la reserva puede competir con un correo ya en vuelo; la verificación de cuenta siempre exige el código vigente.

Las pruebas locales cubren el protocolo HTTP y la cola con un proveedor simulado. Para verificar entrega real se requieren credenciales, remitente autorizado y una prueba de despliegue; no se ha enviado correo externo en estas pruebas.

## Referencias

API de envío: https://resend.com/docs/api-reference/emails/send-email

Idempotencia: https://resend.com/docs/dashboard/emails/idempotency-keys
