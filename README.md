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
    - name: Display environment
      debug:
        msg: "Environment: {{ env_name }}"

    - name: Display port
      debug:
        msg: "Port: {{ app_port }}"

    - name: Simulate deployment
      shell: echo "Deploying on {{ env_name }}"

    - name: Block production deployment
      fail:
        msg: "Deployment blocked: maintenance mode enabled"
      when: maintenance_mode
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

### Fichier `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD with Ansible

on:
  push:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Tests OK"

  deploy-dev:
    needs: test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: sudo apt update && sudo apt install -y ansible
      - run: ansible-playbook -i ansible/inventory/dev.ini ansible/playbook.yml

  deploy-prod:
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
1. Vérifier que le déploiement DEV fonctionne automatiquement
2. Constater l’échec volontaire du déploiement PROD
3. Désactiver temporairement `maintenance_mode` pour autoriser PROD
4. Justifier cette modification

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
