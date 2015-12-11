#ifndef CLIENT_H
#define CLIENT_H

#include <QObject>

class Client : public QObject
{
	Q_OBJECT
public:
	Client(QString *host);
private:
	QString *host;
};

#endif
