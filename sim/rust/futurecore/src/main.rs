use clap::Parser;
use futurecore::init_sim;
use std::{path::PathBuf, process::ExitCode};

/// Rust based Verilator simulation for the Future Core
#[derive(Parser)]
#[clap(version, about, long_about = None)]
struct Cli {
    /// Enable trace to FST file
    #[arg(short, long, value_name = "FST FILE")]
    trace: Option<PathBuf>,
    /// The path to the image file to load into memory
    image: Option<PathBuf>,
}

fn main() -> ExitCode {
    let cli = Cli::parse();
    let executor = match init_sim(cli.trace.as_deref(), cli.image.as_deref()) {
        Ok(executor) => executor,
        Err(e) => {
            eprintln!("error: Error initializing simulation: {}", e);
            return ExitCode::FAILURE;
        }
    };

    if let Err(e) = futurecore::run(executor) {
        eprintln!("error: Error running simulation.");
        return e;
    }

    ExitCode::SUCCESS
}
