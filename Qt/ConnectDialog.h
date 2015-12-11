#ifndef CONNECT_DIALOG_H
#define CONNECT_DIALOG_H

#include <QDialog>

class QLabel;
class QLineEdit;
class QPushButton;
class QHBoxLayout;
class QVBoxLayout;

class ConnectDialog : public QDialog
{
	Q_OBJECT
public:
	ConnectDialog();
signals:
	void newClient(const QString &hostAddress);
protected slots:
	void okButtonClicked();
private:
	QLabel *ipLabel;
	QLineEdit *ipEdit;
	QPushButton *okButton, *exitButton;
	QHBoxLayout *topLayout, *buttonLayout;
	QVBoxLayout *mainLayout;
};

#endif
