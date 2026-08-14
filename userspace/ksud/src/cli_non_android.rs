use anyhow::Result;
use clap::Parser;

use crate::boot_patch::{BootPatchArgs, BootRestoreArgs};
use crate::lkm_image::BootPatchV2Args;
use crate::{apk_sign, defs};

/// KernelSU cli for non-android
#[derive(Parser, Debug)]
#[command(author, version = defs::VERSION_NAME, about, long_about = None)]
struct Args {
    #[command(subcommand)]
    command: Commands,
}

#[derive(clap::Subcommand, Debug)]
enum Commands {
    /// Patch boot or init_boot images to apply KernelSU
    BootPatch(BootPatchArgs),

    /// Restore boot or init_boot images patched by KernelSU
    BootRestore(BootRestoreArgs),

    /// Patch KernelSU into a boot image
    ///
    /// Always operates on a boot image; never selects init_boot or vendor_boot.
    BootPatchV2(BootPatchV2Args),

    /// Get apk size and hash
    GetSign {
        /// apk path
        apk: String,
    },

    /// show supported kmi versions
    SupportedKmis,

    /// Manage KPatch-Next Modules (KPM)
    Kpm {
        #[command(subcommand)]
        command: Kpm,
    },
}

#[derive(clap::Subcommand, Debug)]
enum Kpm {
    /// Load a KPatch-Next Module from a KPM ELF file
    Load {
        /// KPM file path
        path: std::path::PathBuf,
        /// Arguments passed to the KPM
        #[arg(trailing_var_arg = true, allow_hyphen_values = true, num_args = 0..)]
        args: Vec<String>,
    },
    /// Unload a KPatch-Next Module by name
    Unload {
        /// KPM name
        name: String,
    },
    /// Control a KPatch-Next Module by name
    Ctl0 {
        /// KPM name
        name: String,
        /// Control arguments
        ctl_args: String,
    },
    /// Get the number of loaded KPatch-Next Modules
    Num,
    /// List loaded KPatch-Next Modules
    List,
    /// Get detailed information about a KPatch-Next Module
    Info {
        /// KPM name
        name: String,
    },
}

pub fn run() -> Result<()> {
    env_logger::init();

    let cli = Args::parse();

    log::info!("command: {:?}", cli.command);

    let result = match cli.command {
        Commands::GetSign { apk } => {
            let sign = apk_sign::get_apk_signature(&apk)?;
            println!("size: {:#x}, hash: {}", sign.0, sign.1);
            Ok(())
        }

        Commands::BootPatch(boot_patch) => crate::boot_patch::patch(boot_patch),

        Commands::BootRestore(boot_restore) => crate::boot_patch::restore(boot_restore),

        Commands::BootPatchV2(patch) => crate::lkm_image::patch_boot(&patch),

        Commands::SupportedKmis => {
            let kmi = crate::assets::list_supported_kmi();
            for kmi in &kmi {
                println!("{kmi}");
            }
            Ok(())
        }

        Commands::Kpm { command } => match command {
            Kpm::Load { path, args } => {
                let joined_args = args.join(" ");
                crate::kpm::load(
                    &path.to_string_lossy(),
                    if joined_args.is_empty() {
                        None
                    } else {
                        Some(&joined_args)
                    },
                )
            }
            Kpm::Unload { name } => crate::kpm::unload(&name),
            Kpm::Ctl0 { name, ctl_args } => crate::kpm::control(&name, &ctl_args).map(|out| {
                if !out.is_empty() {
                    println!("{out}");
                }
            }),
            Kpm::Num => crate::kpm::nums().map(|n| println!("{n}")),
            Kpm::List => crate::kpm::list().map(|out| {
                if !out.is_empty() {
                    println!("{out}");
                }
            }),
            Kpm::Info { name } => crate::kpm::info(&name).map(|out| {
                if !out.is_empty() {
                    println!("{out}");
                }
            }),
        },
    };

    if let Err(e) = &result {
        log::error!("Error: {e:?}");
    }
    result
}
