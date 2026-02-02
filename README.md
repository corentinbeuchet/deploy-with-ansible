# 🧪 Exercice 4 – Gestion des configurations et des environnements avec Ansible & GitHub Actions

## 📚 Contexte
Cet exercice fait suite aux exercices précédents :
- Exercice 2 : Pipeline CI/CD
- Exercice 3 : Intégration de tests automatisés

Le projet dispose désormais :
- d’un pipeline CI/CD GitHub Actions fonctionnel
- de tests automatisés
- d’une branche `main` protégée

L’objectif de cet exercice est d’introduire **Ansible** pour automatiser la configuration et le déploiement, en gérant **plusieurs environnements**.


---

## 🧩 Partie 0 – Installation d’Ansible

Avant de commencer l’exercice, assurez-vous qu’Ansible est installé sur votre machine ou sur l’environnement CI.

### 🔹 Installation sur Linux (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install -y ansible
```

### 🔹 Installation sur macOS (Homebrew)
```bash
brew install ansible
```

### 🔹 Installation windows
```bash
wsl --install
```
Redémarrer si Ubuntu ne se lance pas
Ouvre le menu Démarrer → cherche “Ubuntu” → lance-le
(ou bien dans PowerShell :)
```bash
wsl -d Ubuntu
sudo apt update
sudo apt install -y ansible
```

### 🔹 Vérification de l’installation
```bash
ansible --version
ansible-playbook --version
```

Vous devez voir s’afficher la version d’Ansible installée.

---
---

## 🎯 Objectifs pédagogiques
À l’issue de cet exercice, vous serez capable de :
- Comprendre l’intérêt de la gestion de configuration en CI/CD
- Utiliser Ansible dans un pipeline GitHub Actions
- Gérer plusieurs environnements (dev / test / prod)
- Séparer le code applicatif de la configuration
- Déployer automatiquement après les tests

---

## 🧩 Partie 1 – Concepts de gestion de configuration

### Problématique
Sans gestion de configuration :
- Déploiements manuels
- Incohérences entre environnements
- Risque d’erreurs élevé

### Principe clé
- Même code pour tous les environnements
- Configuration différente selon l’environnement
- Infrastructure as Code (IaC)

---

## 🧩 Partie 2 – Mise en place d’Ansible

### Structure attendue
```
ansible/
├── inventory/
│   ├── dev.ini
│   ├── test.ini
│   └── prod.ini
├── group_vars/
│   ├── dev.yml
│   ├── test.yml
│   └── prod.yml
└── playbook.yml
```

---

## 🧩 Partie 3 – Gestion des environnements

### Inventaires

**dev.ini**
```ini
[dev]
localhost ansible_connection=local
```

**test.ini**
```ini
[test]
localhost ansible_connection=local
```

**prod.ini**
```ini
[prod]
localhost ansible_connection=local
```

### Variables par environnement

**dev.yml**
```yaml
env_name: development
app_port: 8080
debug_mode: true
maintenance_mode: false
```

**test.yml**
```yaml
env_name: testing
app_port: 8081
debug_mode: false
maintenance_mode: false
```

**prod.yml**
```yaml
env_name: production
app_port: 80
debug_mode: false
maintenance_mode: true
```

---

## 🧩 Partie 4 – Playbook Ansible

```yaml
- name: Deploy application
  hosts: all
  gather_facts: false

  tasks:
    - name: Display environment name
      debug:
        msg: "Environment: {{ env_name }}"

    - name: Display application port
      debug:
        msg: "Application will run on port {{ app_port }}"

    - name: Display debug mode
      debug:
        msg: "Debug mode enabled: {{ debug_mode }}"

    - name: Simulate deployment
      shell: |
        echo "Deploying application..."
        echo "Environment={{ env_name }}"
        echo "Port={{ app_port }}"

    - name: Prevent deployment if maintenance mode is enabled
      fail:
        msg: "Deployment blocked: maintenance mode enabled"
      when: maintenance_mode | default(false)
```

---

## 🧩 Partie 5 – Exécution locale

```bash
ansible-playbook -i ansible/inventory/dev.ini ansible/playbook.yml
ansible-playbook -i ansible/inventory/test.ini ansible/playbook.yml
ansible-playbook -i ansible/inventory/prod.ini ansible/playbook.yml
```

---

## 🧩 Partie 6 – Intégration CI/CD avec GitHub Actions

### Objectif
Automatiser le déploiement après les tests.

### Fichier `.github/workflows/ci.yml` renommé en `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD with Ansible

on:
  push:
    branches: [ main, develop ]
  pull_request:

# Prevent multiple runs piling up for the same branch/PR
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  test:
    name: Run tests (Pull Request)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '25'
      - name: Grant execute permission for Gradle wrapper
        run: chmod +x ./gradlew
      - name: Lancer les tests
        run: ./gradlew test

  deploy-test:
    name: Deploy TEST (Pull Request)
    needs: test
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: sudo apt update && sudo apt install -y ansible
      - run: ansible-playbook -i ansible/inventory/test.ini ansible/playbook.yml

  deploy-dev:
    name: Deploy DEV (On push on develop branch)
    needs: test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: sudo apt update && sudo apt install -y ansible
      - run: ansible-playbook -i ansible/inventory/dev.ini ansible/playbook.yml

  deploy-prod:
    name: Deploy PROD (On push on main branch)
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: sudo apt update && sudo apt install -y ansible
      - run: ansible-playbook -i ansible/inventory/prod.ini ansible/playbook.yml
```

---

## 🧪 Partie 7 – Exercice pédagogique (OBLIGATOIRE)

### Objectif
Mettre en place un déploiement sécurisé en production.

### Travail demandé
1. Toute **Pull Request** déclenche un déploiement en **TEST**
2. Un push sur `develop` déclenche un déploiement en **DEV**
3. Un push sur `main` déclenche un déploiement en **PROD**
4. Constater l’échec volontaire du déploiement PROD
5. Désactiver temporairement `maintenance_mode` pour autoriser PROD
6. Justifier cette modification
7. Rajouter les règles pour protéger le merge sur `main` et `develop`

### Résultats attendus
- DEV : déploiement réussi
- PROD : déploiement bloqué par défaut
- PROD autorisé uniquement après modification consciente

---

## ❓ Questions de réflexion
1. Pourquoi séparer code et configuration ?
2. Pourquoi utiliser plusieurs environnements ?
3. Pourquoi bloquer la production par défaut ?
4. Peut-on utiliser un seul playbook pour tous les environnements ?
5. Quels risques en cas de déploiement manuel ?

---

## 🏁 Conclusion
Cet exercice reproduit un scénario CI/CD réel avec Ansible.
