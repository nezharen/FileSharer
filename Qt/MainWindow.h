#ifndef MAIN_WINDOW_H
#define MAIN_WINDOW_H

#include <QMainWindow>

class ConnectDialog;
class MainServer;

class MainWindow : public QMainWindow
{
	Q_OBJECT
public:
	MainWindow();
protected slots:
	void connectHost();
private:
	QMenu *startMenu;
	QAction *connectAction;
	ConnectDialog *connectDialog;
	MainServer *mainServer;
};

#endif
