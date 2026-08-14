// SPDX-License-Identifier: GPL-2.0-or-later
/*
 * Copyright (C) 2026 rifsxd.
 * All Rights Reserved.
 */

#include <syscall.h>
#include <uapi/scdefs.h>
#include <symbol.h>
#include <linux/printk.h>
#include <kputils.h>
#include <hook.h>
#include <stddef.h>

static int rehook_enabled = 0;
\
static int getpriority_nr = __NR_getpriority;
static int getpriority_nr_arg = 2;

static void getpriority_before(hook_fargs2_t *args, void *udata)
{
    if (current_uid() != 0u) // only allow root 
        return;
}

static inline bool syscall_is_valid(int nr)
{
    void **table;
    
    table = (void **)sys_call_table;
    if (!table || table[nr] == NULL)
        return false;
    
    return true;
}

static void hook_getpriority_syscall(void)
{   
    logki("hook_getpriority_syscall start\n");

    if (!syscall_is_valid(getpriority_nr)) {
        logkfd("skipping invalid native syscall %d\n", getpriority_nr);
        return;
    }
    
    hook_syscalln(getpriority_nr, getpriority_nr_arg, getpriority_before, 0, 0);
    logkfd("hooked native syscall %d\n", getpriority_nr);
    
    logki("hook_getpriority_syscall done\n");
}

static void unhook_getpriority_syscall(void)
{    
    logki("unhook_getpriority_syscall start\n");

    if (!syscall_is_valid(getpriority_nr)) {
        logkfd("skipping invalid native syscall %d\n", getpriority_nr);
        return;
    }

    unhook_syscalln(getpriority_nr, getpriority_before, 0);
    logkfd("unhooked native syscall %d\n", getpriority_nr);

    logki("unhook_getpriority_syscall done\n");
}

/* ---------------- Init / Exit ---------------- */
int rehook_init(void)
{
    logki("rehook_init start\n");

    hook_getpriority_syscall();

    rehook_enabled = 1;

    logki("rehook_init complete\n");
    return 0;
}
KP_EXPORT_SYMBOL(rehook_init);

int rehook_exit(void)
{
    logki("rehook_exit start\n");

    unhook_getpriority_syscall();

    rehook_enabled = 0;

    logki("rehook_exit complete\n");
    return 0;
}
KP_EXPORT_SYMBOL(rehook_exit);

int rehook_status(void)
{
    return rehook_enabled;
}
KP_EXPORT_SYMBOL(rehook_status);