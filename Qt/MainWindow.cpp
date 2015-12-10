#include <QtGui>
#include "MainWindow.h"
#include "ConnectDialog.h"
#include "MainServer.h"

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
	mainServer = new MainServer();
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
