@echo off
echo Démarrage optimisé de BuckPal en mode développement...
echo.
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring.devtools.restart.enabled=false