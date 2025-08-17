run your application with different profiles:
For local development (with VPN):
bash

mvn spring-boot:run -Dspring-boot.run.profiles=local


For Supabase (without VPN):
bash
mvn spring-boot:run -Dspring-boot.run.profiles=supabase