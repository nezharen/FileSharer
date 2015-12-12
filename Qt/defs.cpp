#include "defs.h"

int getInt(const char *s, int start, int end)
{
	int ans = 0;
	for (int i = start; i <= end; i++)
		ans = ans * 10 + (s[i] - '0');
	return ans;
}

