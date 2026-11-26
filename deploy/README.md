# Deployment files

Argo CD reads this folder. `develop` and `main` are separate environments, each with its own root application: `root-develop.yml` follows the `develop` branch and `root-production.yml` follows `main`. Apply a root file once with `kubectl apply -f`, and after that change the cluster only through pull requests.
