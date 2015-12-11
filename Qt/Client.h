#ifndef CLIENT_H
#define CLIENT_H

#include <QObject>

class QTcpSocket;

class Client : public QObject
{
	Q_OBJECT
public:
	Client(QString host);
	void connectHost();
	QString host;
	int status;
signals:
	void statusChanged();
protected slots:
	void connected();
private:
	QTcpSocket *socket;
};

#endif
