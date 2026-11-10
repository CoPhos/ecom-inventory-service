FROM mcr.microsoft.com/mssql/server:2025-latest

USER root

COPY ./init/init-db.sql /usr/src/app/init-db.sql
COPY ./init/init-db.sh /usr/src/app/init-db.sh

RUN chmod +x /usr/src/app/init-db.sh

USER mssql

CMD ["/usr/src/app/init-db.sh"]
