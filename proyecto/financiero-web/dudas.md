Sistemas Distribuidos - Proyecto final
Guerrero Sánchez Diego Isaac
Preguntas hechas al profe

¿Se pueden hacer transferencias a los usuarios generados por el programa?
Sí.

¿Cómo se identifica a los usuarios cuando queremos hacerles una transferencia?, ¿Con su curp?
Sí, con el curp.

¿El servicio Pub/Sub para transacciones es bidireccional?, es decir ¿Son 2 servicios?
Sí, deben ser 2 servicios distintos.

¿Qué debe llevar un balance?, ¿Monto total?
Solo el monto total.

¿Cuál es el periodo de tiempo para la interfaz del admin?
A nuestra consideración.

¿Los tokens se destruyen cuando termina la ejecución del programa que simula a los clientes?
Sí, se destruyen después de cierto tiempo de duración, es a nuestra consideración.

¿Las credenciales y tokens de los clientes de la interfaz web se guardan en algún lugar?
No, se destruyen cuando termina le ejecución del servidor.

¿El servicio de transacciones escucha mensajes de la cola Pub/Sub?, ¿Es un suscriptor? (Lo mismo para el servicio de auditoría)
Sí, es un suscriptor del servicio Pub/Sub.

¿Qué se debe mostrar en la interfaz del usuario?, es decir ¿Qué es Log y timestamps?
El tiempo igualmente es a nuestra consideración, solo es el historial de transacciones hechas a su cuenta.

¿GCP debe levantar automáticamente las instancias (se debe usar autohealing) o nosotros las levantamos?
No se va a utilizar autohealing, va a ser manual, vamos a apagar y volver a iniciar las instancias para simular la caída del servicio.