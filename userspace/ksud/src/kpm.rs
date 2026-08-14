use anyhow::{Context, bail};
use std::ffi::CString;
use std::os::raw::c_void;

const __NR_SUPERCALL: i64 = 45;

const SUPERCALL_HELLO: i64 = 0x1000;
const SUPERCALL_KERNELPATCH_VER: i64 = 0x1008;
const SUPERCALL_KPM_LOAD: i64 = 0x1020;
const SUPERCALL_KPM_UNLOAD: i64 = 0x1021;
const SUPERCALL_KPM_CONTROL: i64 = 0x1022;
const SUPERCALL_KPM_NUMS: i64 = 0x1030;
const SUPERCALL_KPM_LIST: i64 = 0x1031;
const SUPERCALL_KPM_INFO: i64 = 0x1032;

const KP_MAJOR: i64 = 0;
const KP_MINOR: i64 = 13;
const KP_PATCH: i64 = 5;

const KPM_INFO_BUF: usize = 4096;

#[allow(dead_code)]
const SUPERCALL_HELLO_MAGIC: i64 = 0x2026_2026;

const fn ver_and_cmd(cmd: i64) -> i64 {
    let version_code = (KP_MAJOR << 16) + (KP_MINOR << 8) + KP_PATCH;
    (version_code << 32) | (0x2026 << 16) | (cmd & 0xFFFF)
}

fn compact_cmd(cmd: i64) -> i64 {
    let ver = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            ver_and_cmd(SUPERCALL_KERNELPATCH_VER),
        )
    };
    if ver >= 0xa05 { ver_and_cmd(cmd) } else { cmd }
}

fn read_cstr(buf: &[u8]) -> String {
    let len = buf.iter().position(|&b| b == 0).unwrap_or(buf.len());
    String::from_utf8_lossy(&buf[..len]).into_owned()
}

#[allow(dead_code)]
pub fn available() -> bool {
    let cmd = compact_cmd(SUPERCALL_HELLO);
    let ret = unsafe { libc::syscall(__NR_SUPERCALL, std::ptr::null::<c_void>(), cmd) };
    ret == SUPERCALL_HELLO_MAGIC
}

pub fn load(path: &str, args: Option<&str>) -> anyhow::Result<()> {
    let c_path = CString::new(path).context("invalid kpm path")?;
    let c_args = args
        .filter(|a| !a.is_empty())
        .map(CString::new)
        .transpose()
        .context("invalid kpm args")?;
    let args_ptr = c_args.as_ref().map_or(std::ptr::null(), |c| c.as_ptr());
    let cmd = compact_cmd(SUPERCALL_KPM_LOAD);
    let ret = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            cmd,
            c_path.as_ptr(),
            args_ptr,
            std::ptr::null::<c_void>(),
        )
    };
    if ret < 0 {
        bail!("kpm load failed: {ret}");
    }
    Ok(())
}

pub fn unload(name: &str) -> anyhow::Result<()> {
    let c_name = CString::new(name).context("invalid kpm name")?;
    let cmd = compact_cmd(SUPERCALL_KPM_UNLOAD);
    let ret = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            cmd,
            c_name.as_ptr(),
            std::ptr::null::<c_void>(),
        )
    };
    if ret < 0 {
        bail!("kpm unload failed: {ret}");
    }
    Ok(())
}

pub fn control(name: &str, ctl_args: &str) -> anyhow::Result<String> {
    let c_name = CString::new(name).context("invalid kpm name")?;
    let c_args = CString::new(ctl_args).context("invalid control args")?;
    let mut buf = vec![0u8; KPM_INFO_BUF];
    let cmd = compact_cmd(SUPERCALL_KPM_CONTROL);
    let ret = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            cmd,
            c_name.as_ptr(),
            c_args.as_ptr(),
            buf.as_mut_ptr().cast::<c_void>(),
            buf.len() as i64,
        )
    };
    if ret < 0 {
        bail!("kpm control failed: {ret}");
    }
    Ok(read_cstr(&buf))
}

pub fn nums() -> anyhow::Result<i64> {
    let cmd = compact_cmd(SUPERCALL_KPM_NUMS);
    let ret = unsafe { libc::syscall(__NR_SUPERCALL, std::ptr::null::<c_void>(), cmd) };
    if ret < 0 {
        bail!("kpm nums failed: {ret}");
    }
    Ok(ret)
}

pub fn list() -> anyhow::Result<String> {
    let mut buf = vec![0u8; KPM_INFO_BUF];
    let cmd = compact_cmd(SUPERCALL_KPM_LIST);
    let ret = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            cmd,
            buf.as_mut_ptr().cast::<c_void>(),
            buf.len() as i64,
        )
    };
    if ret < 0 {
        bail!("kpm list failed: {ret}");
    }
    Ok(read_cstr(&buf))
}

pub fn info(name: &str) -> anyhow::Result<String> {
    let c_name = CString::new(name).context("invalid kpm name")?;
    let mut buf = vec![0u8; KPM_INFO_BUF];
    let cmd = compact_cmd(SUPERCALL_KPM_INFO);
    let ret = unsafe {
        libc::syscall(
            __NR_SUPERCALL,
            std::ptr::null::<c_void>(),
            cmd,
            c_name.as_ptr(),
            buf.as_mut_ptr().cast::<c_void>(),
            buf.len() as i64,
        )
    };
    if ret < 0 {
        bail!("kpm info failed: {ret}");
    }
    Ok(read_cstr(&buf))
}
