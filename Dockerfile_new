FROM ccr.ccs.tencentyun.com/io.eden/filepreview-base:v1.0.0
LABEL maintainer="eden.opt@gmail.com"

ADD jodconverter-web/target/kkFileView-*.tar.gz /opt/
ENV KKFILEVIEW_BIN_FOLDER /opt/kkFileView-2.2.0-SNAPSHOT/bin
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider", "-Dspring.config.location=/opt/kkFileView-2.2.0-SNAPSHOT/config/application.properties", "-jar", "/opt/kkFileView-2.2.0-SNAPSHOT/bin/kkFileView-2.2.0-SNAPSHOT.jar"]