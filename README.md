# 🚀 Exercice 4 – Conteneuriser et déployer sur plusieurs environnements (Docker, Ansible & GitHub Actions)

## 📚 Contexte
Cet exercice fait suite aux exercices précédents :
- Exercice 2 : workflow GitHub sécurisé (CI, protection de branche, revue)
- Exercice 3 : tests automatisés, livrable, qualité et performance

Votre projet dispose désormais :
- d'un pipeline GitHub Actions qui teste, vérifie et produit un livrable (`app.jar`)
- d'une branche `main` protégée

Il manque la partie **CD** : **livrer** et **déployer**. Vous allez :
1. emballer l'application dans une **image Docker**, pour qu'elle tourne partout de la même façon ;
2. utiliser **Ansible** pour déployer sur **plusieurs environnements** (dev / test / prod), avec une configuration différente pour chacun.

> Les « serveurs » de cet exercice sont simulés (`localhost`) : le but est de comprendre la mécanique, pas d'administrer de vraies machines.

---

## 🎯 Ce que vous devez comprendre et savoir faire
À la fin de cet exercice, vous devez être capable de :
- **Expliquer le problème du « ça marche sur ma machine »** et comment une image Docker le résout.
- **Écrire un Dockerfile simple**, construire l'image et la lancer.
- **Faire un test de fumée** (*smoke test*) : vérifier automatiquement que l'application démarrée répond.
- **Séparer le code de la configuration** : même code, même image, configuration différente par environnement.
- **Lire et exécuter un playbook Ansible** (inventaire, variables, tâches).
- **Expliquer pourquoi on promeut le même artefact** de dev jusqu'en prod, au lieu de le reconstruire.
- **Relier une branche à un environnement** : PR → test, `develop` → dev, `main` → prod.
- **Justifier un garde-fou** avant la production, et savoir le lever de façon consciente et tracée.

---

## 🧩 Partie 0 – Point de départ et installations

### Point de départ
Continuez **dans votre dépôt de l'exercice 3** (`automated-tests`) : c'est votre pipeline que vous faites évoluer.

> Exercice 3 non terminé ? Partez de [ce dépôt](https://github.com/corentinbeuchet/deploy-with-ansible) : clonez-le, puis faites pointer `origin` vers un nouveau dépôt **public** vide à vous, comme à l'exercice 1 (`git remote rename origin upstream`, `git remote add origin …`, `git push -u origin main`). Protégez ensuite `main` comme à l'exercice 3.
>
> ⚠️ Avant votre premier push, redonnez à `gradlew` son droit d'exécution (le piège de l'exercice 3) : `git update-index --chmod=+x gradlew`, puis `git commit -m "fix: make gradlew executable"`.

### Docker
Installez **Docker Desktop** (Windows, macOS) ou **Docker Engine** (Linux) : 👉 https://docs.docker.com/get-started/get-docker/

```bash
docker --version
```

> Pas de Docker possible sur votre poste ? Vous pouvez faire les parties Docker **uniquement dans la CI** : les runners GitHub ont Docker préinstallé.

### Ansible
La méthode recommandée par la documentation officielle est `pipx`, qui installe la dernière version stable.

**Linux (Ubuntu / Debian)**
```bash
sudo apt update
sudo apt install -y pipx
pipx ensurepath
pipx install --include-deps ansible
```
Fermez puis rouvrez le terminal.

**macOS (Homebrew)**
```bash
brew install ansible
```

**Windows** : Ansible ne tourne pas nativement sous Windows, on passe par WSL (un Linux intégré à Windows).
```powershell
wsl --install
```
Redémarrez si demandé, puis lancez **Ubuntu** depuis le menu Démarrer et suivez les instructions **Linux** ci-dessus.

**Vérification**
```bash
ansible --version
ansible-playbook --version
```

---

## 🧩 Partie 1 – Concepts

### Sans automatisation du déploiement
- Déploiements manuels, différents d'une personne à l'autre
- Environnements qui divergent (« en test ça marchait… »)
- Risque d'erreur élevé, et personne ne sait exactement ce qui tourne en prod

### Principes clés
- **Un seul artefact** (l'image Docker), construit une fois par la CI, promu d'environnement en environnement
- **Même code partout, configuration différente** selon l'environnement
- **Infrastructure as Code (IaC)** : la configuration est versionnée dans Git, relue en PR, rejouable

---

## 🧩 Partie 2 – Conteneuriser l'application (Docker)

Travaillez sur une branche :
```bash
git switch main
git pull
git switch -c feat/docker
```

### Le Dockerfile
À la racine du projet, créez `Dockerfile` :

```dockerfile
# Image de base : Java 25 (LTS), uniquement l'environnement d'exécution (JRE)
FROM eclipse-temurin:25-jre
WORKDIR /app
# Le jar construit (et testé) par Gradle
COPY build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Créez aussi `.dockerignore`, pour ne pas envoyer tout le projet à Docker :
```
.git
.gradle
.idea
src
```

> Le jar s'appelle `app.jar` grâce au bloc `bootJar` ajouté à l'exercice 3 (partie 6).

### Construire et lancer en local
```bash
./gradlew build
docker build -t demo:local .
docker run --rm -p 8080:8080 demo:local
```
Ouvrez 👉 http://localhost:8080/hello, puis arrêtez le conteneur (`Ctrl+C`).

📌 Cette image contient **tout** ce qu'il faut pour lancer l'application : n'importe quelle machine avec Docker la lancera à l'identique.

---

## 🧩 Partie 3 – Mise en place d'Ansible

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

### Inventaires : *où* déployer

**inventory/dev.ini**
```ini
[dev]
localhost ansible_connection=local
```

**inventory/test.ini**
```ini
[test]
localhost ansible_connection=local
```

**inventory/prod.ini**
```ini
[prod]
localhost ansible_connection=local
```

### Variables par environnement : *avec quelle configuration*
Ansible charge automatiquement `group_vars/<groupe>.yml` pour les machines du groupe `[<groupe>]`.

**group_vars/dev.yml**
```yaml
env_name: development
app_port: 8080
debug_mode: true
maintenance_mode: false
```

**group_vars/test.yml**
```yaml
env_name: testing
app_port: 8081
debug_mode: false
maintenance_mode: false
```

**group_vars/prod.yml**
```yaml
env_name: production
app_port: 80
debug_mode: false
maintenance_mode: true
```

### Playbook : *quoi faire*

**playbook.yml**
```yaml
- name: Deploy application
  hosts: all
  gather_facts: false

  vars:
    image_tag: local   # remplacé par la CI (-e image_tag=...)

  tasks:
    - name: Refuser le déploiement si l'environnement est en maintenance
      ansible.builtin.fail:
        msg: "Deployment blocked: maintenance mode enabled on {{ env_name }}"
      when: maintenance_mode | default(false)

    - name: Afficher la configuration
      ansible.builtin.debug:
        msg: "Env={{ env_name }} | port={{ app_port }} | debug={{ debug_mode }}"

    - name: Simuler le déploiement de l'image
      ansible.builtin.shell: |
        echo "Deploying image demo:{{ image_tag }}"
        echo "Environment={{ env_name }}"
        echo "Port={{ app_port }}"
```

📌 Le garde-fou (`fail`) est **la première tâche** : si l'environnement est en maintenance, on s'arrête **avant** de toucher à quoi que ce soit.

### Exécution locale
```bash
ansible-playbook -i ansible/inventory/dev.ini  ansible/playbook.yml
ansible-playbook -i ansible/inventory/test.ini ansible/playbook.yml
ansible-playbook -i ansible/inventory/prod.ini ansible/playbook.yml
```
Résultat attendu : dev et test réussissent, **prod échoue** (maintenance).

---

## 🧩 Partie 4 – Le pipeline CI/CD complet

### Renommer le workflow
Le fichier ne fait plus seulement de la CI :
```bash
git mv .github/workflows/ci.yml .github/workflows/ci-cd.yml
```

### Contenu de `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD

on:
  push:
    branches: [ main, develop ]
  pull_request:

# Évite d'empiler plusieurs runs pour la même branche / PR
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  # ---------- CI : exercice 3 ----------
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '25'
      - name: Construire et vérifier
        run: ./gradlew build
      - name: Publier le livrable
        uses: actions/upload-artifact@v7
        with:
          name: app
          path: build/libs/app.jar
      - name: Publier les rapports (même en cas d'échec)
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: rapports
          path: build/reports/

  # (gardez ici votre job "performance" de l'exercice 3, inchangé)

  # ---------- Livraison : image Docker ----------
  docker:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - name: Récupérer le livrable testé
        uses: actions/download-artifact@v8
        with:
          name: app
          path: build/libs
      - name: Construire l'image
        run: docker build -t demo:${{ github.sha }} .
      - name: Lancer le conteneur
        run: docker run -d --name demo -p 8080:8080 demo:${{ github.sha }}
      - name: Test de fumée
        run: |
          for i in $(seq 1 30); do
            curl -fs http://localhost:8080/hello && exit 0
            sleep 2
          done
          docker logs demo
          exit 1

  # ---------- Déploiements ----------
  deploy-test:
    name: Deploy TEST (Pull Request)
    needs: docker
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - run: pipx install --force ansible-core
      - run: ansible-playbook -i ansible/inventory/test.ini ansible/playbook.yml -e image_tag=${{ github.sha }}

  deploy-dev:
    name: Deploy DEV (push sur develop)
    needs: docker
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - run: pipx install --force ansible-core
      - run: ansible-playbook -i ansible/inventory/dev.ini ansible/playbook.yml -e image_tag=${{ github.sha }}

  deploy-prod:
    name: Deploy PROD (push sur main)
    needs: docker
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - run: pipx install --force ansible-core
      - run: ansible-playbook -i ansible/inventory/prod.ini ansible/playbook.yml -e image_tag=${{ github.sha }}
```

📌 À lire dans ce fichier :
- `needs:` fixe l'**ordre** : pas d'image si le build échoue, pas de déploiement si le test de fumée échoue.
- `if:` relie un **événement** à un **environnement**.
- `image_tag` = le hash du commit : on sait exactement **quelle version** est déployée où.
- `pipx install --force ansible-core` installe la dernière version (les runners en ont déjà une, parfois plus ancienne). `ansible-core` suffit ici : le playbook n'utilise que des modules intégrés (`ansible.builtin`).

> Si vous avez gardé le job `performance`, ajoutez-le aux `needs` du job `docker` : `needs: [build, performance]`.

Committez tout (Dockerfile, `.dockerignore`, `ansible/`, workflow), poussez `feat/docker` et ouvrez une PR vers `main`. Ne la mergez pas encore.

---

## 🧪 Partie 5 – Travail demandé

### 1. Observer la PR
Dans l'onglet **Checks** de la PR : `build` → `docker` → `Deploy TEST` s'enchaînent. `Deploy DEV` et `Deploy PROD` sont **ignorés** (*skipped*) : ce n'est ni `develop` ni `main`.

### 2. Créer la branche `develop`
Mergez la PR dans `main`, puis :
```bash
git switch main
git pull
git switch -c develop
git push -u origin develop
```
Dans l'onglet **Actions** : le push sur `main` (le merge) a lancé `Deploy PROD` → **échec volontaire** (maintenance). Le push de `develop` a lancé `Deploy DEV` → **succès**.

### 3. Protéger `develop` aussi
Dans **Settings → Branches**, ajoutez une règle pour `develop`, identique à celle de `main` (PR obligatoire, check `build` obligatoire, pas de contournement).

### 4. Autoriser la production, consciemment
Sur une branche partant de `develop`, passez `maintenance_mode: false` dans `group_vars/prod.yml`. Dans la description de la PR, **justifiez** ce changement (pourquoi maintenant, qui a validé).
Mergez dans `develop`, puis ouvrez une PR `develop` → `main` et mergez-la.

### ✅ Résultats attendus
- PR : déploiement en **TEST**
- `develop` : déploiement en **DEV**
- `main` : **PROD bloquée par défaut**, autorisée seulement après une modification relue et justifiée
- Dans les logs de chaque déploiement : le **même** `image_tag` que l'image testée

### ⭐ Bonus – Validation humaine avant la prod
Dans **Settings → Environments**, créez un environnement `production` avec **Required reviewers** (votre binôme ou vous-même). Ajoutez `environment: production` au job `deploy-prod`.
Au prochain push sur `main`, le déploiement **attend une approbation** dans l'onglet **Actions**.

---

## ❓ Questions de réflexion
1. Quel problème concret résout une image Docker ? Qu'est-ce qu'elle ne résout pas ?
2. Pourquoi l'image est-elle construite à partir du jar **téléchargé** (déjà testé) plutôt que recompilée ?
3. Pourquoi séparer code et configuration ?
4. Pourquoi utiliser plusieurs environnements ?
5. Pourquoi bloquer la production par défaut ? Quelle différence entre le `maintenance_mode` et le bonus « Required reviewers » ?
6. Peut-on utiliser un seul playbook pour tous les environnements ? Qu'est-ce qui change d'un environnement à l'autre ?
7. Quels risques en cas de déploiement manuel ?

---

## 🏁 Conclusion
Votre pipeline va maintenant du commit jusqu'à la production :

**build & tests → image Docker → test de fumée → déploiement TEST / DEV / PROD**

Il reste un angle mort : **la sécurité**. C'est l'objet de l'exercice 5.
