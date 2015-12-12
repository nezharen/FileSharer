#include <QtNetwork>
#include <iostream>
#include <cstdio>
#include "defs.h"
#include "Client.h"

Client::Client(QString host)
{
	this->host = host;
	status = CLIENT_STATUS_CONNECTING;
	socket = new QTcpSocket(this);
	connect(socket, SIGNAL(connected()), this, SLOT(connected()));
	connect(socket, SIGNAL(disconnected()), this, SLOT(closeConnection()));
	connect(socket, SIGNAL(readyRead()), this, SLOT(readCommand()));
	file_socket = new QTcpSocket(this);
	connect(file_socket, SIGNAL(connected()), this, SLOT(sendFileContent()));
	commandLength = 0;
}

void Client::connectHost()
{
	socket->connectToHost(host, SERVER_PORT);
}

void Client::connected()
{
	status = CLIENT_STATUS_CONNECTED;
	emit statusChanged();
}

void Client::closeConnection()
{
	emit clientConnectionClosed(this);
}

void Client::readCommand()
{
	QDataStream in(socket);
	in.setVersion(QDataStream::Qt_4_3);
	forever
	{
		if (socket->bytesAvailable() <= 0)
			break;
		int count = in.readRawData(buffer, BUFFER_SIZE);
		for (int i = 0; i < count; i++)
			command[commandLength++] = buffer[i];
		if (command[commandLength - 1] == '\n')
		{
			commandLength--;
			int commandCode = getInt(command, 0, 2);
			int port;
			switch (commandCode)
			{
				case SERVER_COMMAND_ACCEPT_FILE:
					port = getInt(command, 4, commandLength - 1);
					file_socket->connectToHost(host, port);
					break;
				case SERVER_COMMAND_REJECT_FILE:
					status = CLIENT_STATUS_SEND_FILE_FAILED;
					emit statusChanged();
					break;
				case SERVER_COMMAND_RECEIVE_FILE_SUCCESS:
					status = CLIENT_STATUS_SEND_FILE_DONE;
					emit statusChanged();
					break;
				case SERVER_COMMAND_RECEIVE_FILE_FAILED:
					status = CLIENT_STATUS_SEND_FILE_FAILED;
					emit statusChanged();
					break;
				default:
					break;
			}
			commandLength = 0;
		}
	}
}

void Client::sendFile(QString path)
{
	status = CLIENT_STATUS_SENDING_FILE;
	this->path = path;
	emit statusChanged();
	std::string str = path.toStdString();
	const char *filepath = str.c_str();
	int p = strlen(filepath);
	while ((filepath[p] != '/') && (filepath[p] != '\\'))
		p--;
	p++;
	char s[BUFFER_SIZE];
	sprintf(s, "%d ", CLIENT_COMMAND_SEND_FILE);
	for (int i = strlen(s); ; i++)
	{
		s[i] = filepath[p];
		if (filepath[p] == '\0')
		{
			s[i] = '\n';
			s[i + 1] = '\0';
			break;
		}
		p++;
	}
	QByteArray block;
	QDataStream out(&block, QIODevice::WriteOnly);
	out.setVersion(QDataStream::Qt_4_3);
	out.writeRawData(s, strlen(s));
	socket->write(block);
}

void Client::sendFileContent()
{
	QFile file(path);
	if (file.open(QIODevice::ReadOnly))
		while (!file.atEnd())
			file_socket->write(file.read(BUFFER_SIZE));
	file_socket->close();
}
