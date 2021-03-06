# Time-Fluid Field-Based Coordination Through Programmable Distributed Schedulers

This repository contains code and instruction to reproduce the experiments presented in the paper

> Time-Fluid Field-Based Coordination Through Programmable Distributed Schedulers

Submitted to

> [Logical Methods in Computer Science](https://lmcs.episciences.org/)

## Sanity Check and current status

[![DOI](https://zenodo.org/badge/315973644.svg)](https://zenodo.org/badge/latestdoi/315973644)
![](https://github.com/DanySK/Experiment-2020-LMCS-TimeFluid/workflows/CI/badge.svg)
![](https://github.com/DanySK/Experiment-2020-LMCS-TimeFluid/workflows/Docker/badge.svg)
![](https://github.com/DanySK/Experiment-2020-LMCS-TimeFluid/workflows/Generate%20and%20deploy%20charts/badge.svg)

## Organization

This repository is organized as follows:
* `src/main` contains source files, split by languages
* Simulation descriptors are found in `src/main/yaml`
* Build source and executing simulation has been automated via Gradle.
The build and execution procedure is defined in the `build.gradle.kts` and `settings.gradle.kts` files
* `data` contains data generated by us when executing the experiments, it is tracked in order to ease further analyses and make them possible without re-running all the simulations.
* `effects` contains effect files compatible with the legacy Alchemist graphical interface that has been used to develop and debug the experiments.
* `Dockerfile` contains the instructions to build a container image able to guarantee the reproduction of the experiments
* `kubernetes-pod.yml` is a Kubernetes Pod descriptor stub which is provided
to allow those with access to a computing infrastructure to relaunch all the
experiments quickly

## How to reproduce

### A note on performance

A premise to those willing to reproduce the experiment entirely:
you will need a well equipped machine (for 2020 standards, at least).
The system is configured to execute a total of 1800 simulations in order to
generate the whole data presented in the paper.
The time required to complete all such simulations may amount to *weeks*
for systems with low CPU parallelism.

We executed the experiments using the following hardware:

**Development Machine: Alienware Aurora R7**
* Intel Core i7 8700
* 32GB RAM
* Manjaro Linux 20.2 Nibia (using Linux 5.8.18-1-MANJARO x86_64 SMP PREEMPT)

**Batch-test Machine**
* 2x AMD EPYC 7301
* 128GB RAM
* Manjaro Linux 20.2 Nibia (using Linux 5.8.16-2-MANJARO x86_64 SMP PREEMPT)

**Full batch execution machine**
* 4x Intel(R) Xeon(R) Gold 5118 CPU @ 2.30GHz
* 256GB RAM
* Kubernetes node hosted on Clear Linux OS version 34130

#### Working with existing data

In order to speed up the work for those interested in reproducing the plot creation or acquiring further insights on the current experiments,
we track the whole data we ourselves generated when running the batch.
Processing such data and producing charts does not require any particularly powerful system and should complete in a matter of *minutes* on any modern
desktop hardware.

### Common configuration

This experiment relies on a pre-release version of the Alchemist simulator.
The required dependencies are located on a GitHub Packages repository,
which requires authentication even for public repositories.
This has been a long standing issue for github, see:
https://github.community/t/download-from-github-package-registry-without-authentication/14407.
Once the issue will be solved, this experiment will work without any configuration user side.
We provide instructions to configure the environment, but of course it requires the user's credentials.
In order to obtain them, first create a GitHub account if you do not have one.
Then visit: https://github.com/settings/tokens/new
And create a new token with permission at least `read:packages`.
Save the token, you can see it only once and it's not recoverable (but you can generate another one).
From now on, we will refer to this token when mentioning a "GitHub token".

### Custom environment

This section will provide instructions on how to execute the experiments
and create the charts on a "normal" PC.
Pick this option if:
1. You can install and remove software on the machine
2. You want to run the experiments on MacOS X or Windows
3. You want to see the simulations executing on a graphical interface,
change the code, etc.

#### Requirements for executing and working with the simulations

1. A correct Java 11+ installation.
We recommend using one of the two versions which are under test
in our continuous integration system: AdoptOpenJDK Hotspot 11 and AdoptOpenJDK Hotspot 14; or the version in use by our Docker image: AdoptOpenJDK Hotspot 15.
Commands `java -version` and `javac -version` must work and print the correct version.

#### Requirements for data analysis and plotting

1. A correct Python 3 and Pip 3 installation. The experiments used version 3.9.1.
Commands `python --version` and `pip --version` must print the correct version 3.x.y, where x >= 8.

#### Executing simulations

To run the example you can rely on the pre-configured [Gradle](https://gradle.org) build script.
It will automatically download all the required libraries, set up the environment, and execute the simulator via command line for you.
As first step, use `git` to locally clone this repository.
Now, you can feed your credentials to this system in two ways:

Via environment variables:
* set your username in `GITHUB_USERNAME`
* set your token in `GITHUB_TOKEN`

Via Gradle project properties, see: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
* set your username in `githubUsername`
* set your token in `githubToken`

Once done, you will be able to launch the simulations using the tasks named
`run<experiment><mode>` where:
* `<experiment>` can be `Gradient`, `Moving`, `Channel`, or `All`; the latter will execute all other experiments in order.
* `<mode>` can be `Batch` or `Graphic`, the former will start an headless run of all simulations, while the second will launch a single simulation with default values and pop up the graphical interface.

The selected tasks can be launched via Gradle as follows

on UNIX:
```bash
./gradlew run<experiment><mode>
```
On Windows:
```
gradlew.bat run<experiment><mode>
```

If you decide to use the graphical interface, press <kb>P</kb> to start the simulation.
For further information about the gui, see the [graphical interface shortcuts](https://alchemistsimulator.github.io/wiki/usage/gui/).

### Docker-enabled environment

Running using docker requires less configuration,
but provides less flexibility and requires docker to be installed locally.

1. login to the GitHub image registry: `docker login ghcr.io -u <your_github_username> --password <your_github_token>`
2. `docker pull docker.pkg.github.com/danysk/experiment-2020-lmcs-timefluid/lmcs2020-timefluid:latest`
2. Move to a folder with enough space, you will need at least 1GB of free space, with `cd <folderpath>`
3. Create a directory to host the charts: `mkdir charts`
4. Create a directory to host the simulation data: `mkdir data`
5. launch the container: ``docker run -t -i -e GITHUB_USERNAME=<your_github_username> -e GITHUB_TOKEN=<your_github_token> -v "$(pwd)"/charts:/lmcs/charts:rw -v "$(pwd)/"data:/lmcs/data:rw docker.pkg.github.com/danysk/experiment-2020-lmcs-timefluid/lmcs2020-timefluid``

The container will start, run all simulations, put data inside your `data` folder, and once all simulations are complete, process such data and produces the charts in output in the `charts` folder.

#### Experimenting with the docker image

You can open a terminal inside the running container instead of launching simulations and chart generation and run experiments there,
even though we recommend using a local installation to make changes.

You can open the terminal by running:

``docker run -t -i docker.pkg.github.com/danysk/experiment-2020-lmcs-timefluid/lmcs2020-timefluid /bin/bash``

However, we recommend to preserve the environment variables and bind mounts of the batch command, in order to simplify data export from the container:

``docker run -t -i -e GITHUB_USERNAME=<your_github_username> -e GITHUB_TOKEN=<your_github_token> -v "$(pwd)"/charts:/lmcs/charts:rw -v "$(pwd)/"data:/lmcs/data:rw docker.pkg.github.com/danysk/experiment-2020-lmcs-timefluid/lmcs2020-timefluid /bin/bash``

### Kubernetes (K8s) cluster

If you can launch the container on a K8s cluster,
we provide instructions to do so.
Access to powerful and parallel resources can greatly speedup the
execution of the experiments.

This guide will assume that you have the `kubectl` CLI command installed,
working, and configured to issue instructions to the cluster you have
the right to submit pods to.

We provide a pre-configured `kubernetes-pod.yml` file.
In order to use it, you will need to first create a secret to pass your GitHub token and allow the running pod to download packages from GitHub.
You will need such secret for both access to the container image and for
downloading the Alchemist beta packages.

``kubectl create secret generic <packages_secret_name> --from-literal=token=<your_github_token>``

Now, we need a second secret for the registry.

``kubectl create secret docker-registry <registry_secret_name> --docker-username=<your_github_username> --docker-password=<your_github_token> --docker-email=<email_you_registered_to_github_with>``

Replace in `kubernetes-pod.yml`:
* `github-packages-danysk-readonly` with `<packages_secret_name>`
* `github-registry-danysk-readonly` with `<registry_secret_name>`

In case your cluster does not automatically assign the required resources to new pods, edit `kubernetes-pod.yml` to request them following the [K8s guide](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/).

You are now ready to run the container on your cluster:

``kubectl create -f kubernetes-pod.yml``

You can now inspect the outuput with:

``kubectl logs lmcs-2020-timefluid-experiments``

and even open a shell inside the running container:

``kubectl exec --stdin --tty lmcs-2020-timefluid-experiments -- /bin/bash``

The container is pre-configured for sleeping thirty days once it's finished.
This leaves plenty of time to retrieve the run results via, e.g.:

``kubectl cp lmcs-2020-timefluid-experiments:/lmcs/data <destination_folder>``

Once results have been copied out of the running container,
it can be shut down with:

``kubectl delete pod lmcs-2020-timefluid-experiments``

Please keep in mind that doing so will *destroy* the data on the cluster, so make sure that you exported it and saved in some other location.

## License

Released under the terms of the GNU General Public License 3.0
