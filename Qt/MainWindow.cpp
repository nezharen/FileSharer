#include <iostream>
#include <QtGui>
#include <QtNetwork>
#include "defs.h"
#include "Server.h"
#include "Client.h"
#include "ConnectDialog.h"
#include "MainWindow.h"

MainWindow::MainWindow()
{
	connectAction = new QAction(tr("&Connect"), this);
	connectAction->setStatusTip(tr("Connect a host to share file with it."));
	connect(connectAction, SIGNAL(triggered()), this, SLOT(connectHost()));
	startMenu = menuBar()->addMenu(tr("&Start"));
	startMenu->addAction(connectAction);
	statusBar();
	setFixedSize(320, 560);
	setWindowTitle(tr("FileSharer"));
	connectDialog = NULL;
	serverList = new QList<Server*>;
	clientList = new QList<Client*>;
	mainServerSocket = new QTcpServer(this);
	connect(mainServerSocket, SIGNAL(newConnection()), this, SLOT(acceptNewConnection()));
	if (!(mainServerSocket->listen(QHostAddress::Any, SERVER_PORT)))
	{
		std::cerr << "Cannot listen port: " << SERVER_PORT << std::endl;
		qApp->quit();
	}
}

void MainWindow::connectHost()
{
	if (connectDialog == NULL)
	{
		connectDialog = new ConnectDialog;

	}

	connectDialog->show();
	connectDialog->raise();
	connectDialog->activateWindow();
}

void MainWindow::acceptNewConnection()
{
	Server *server = new Server(mainServerSocket->nextPendingConnection());
	serverList->append(server);
}
