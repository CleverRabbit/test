"""
GitHub Automation Module
Создание репозиториев и пуш кода через GitHub API
"""
import os
from github import Github, InputGitTreeElement, InputGitAuthor
from typing import List, Dict, Tuple
import base64


class GitHubAutomation:
    """Автоматизация работы с GitHub"""
    
    def __init__(self):
        self.token = os.getenv("GITHUB_TOKEN", "")
        self.username = os.getenv("GITHUB_USERNAME", "")
        self.gh = Github(self.token) if self.token else None
    
    def create_repository(self, repo_name: str, description: str = "") -> Tuple[bool, str]:
        """
        Создание нового репозитория
        
        Args:
            repo_name: Название репозитория
            description: Описание
        
        Returns:
            (success, message) - успех и сообщение/URL
        """
        if not self.gh:
            return False, "GitHub токен не настроен"
        
        try:
            user = self.gh.get_user()
            repo = user.create_repo(
                name=repo_name,
                description=description or "AI Generated Project",
                private=False,
                auto_init=True  # Создаём с README
            )
            return True, repo.html_url
        except Exception as e:
            return False, f"Ошибка создания репозитория: {str(e)}"
    
    def push_files(self, repo_name: str, files: Dict[str, str], commit_message: str = "Initial commit") -> Tuple[bool, str]:
        """
        Пуш файлов в репозиторий
        
        Args:
            repo_name: Название репозитория
            files: Словарь {path: content}
            commit_message: Сообщение коммита
        
        Returns:
            (success, message)
        """
        if not self.gh:
            return False, "GitHub токен не настроен"
        
        try:
            user = self.gh.get_user()
            repo = user.get_repo(f"{self.username}/{repo_name}")
            
            # Получаем последний коммит
            branch = repo.get_branch("main")
            last_commit = repo.get_git_commit(branch.commit.sha)
            
            # Создаём дерево файлов
            tree_data = []
            for file_path, content in files.items():
                # Кодируем контент в base64
                encoded_content = content.encode('utf-8')
                
                tree_data.append(InputGitTreeElement(
                    path=file_path,
                    mode="100644",
                    type="blob",
                    content=content
                ))
            
            # Создаём новое дерево
            tree = repo.create_git_tree(tree_data, base_tree=last_commit.tree)
            
            # Создаём коммит
            parent = repo.get_git_commit(last_commit.sha)
            new_commit = repo.create_git_commit(
                message=commit_message,
                tree=tree,
                parents=[parent]
            )
            
            # Обновляем ветку
            ref = repo.get_git_ref(f"heads/main")
            ref.edit(sha=new_commit.sha)
            
            return True, f"Успешно запушено {len(files)} файлов"
            
        except Exception as e:
            return False, f"Ошибка пуша файлов: {str(e)}"
    
    def get_repo_url(self, repo_name: str) -> str:
        """Получение URL репозитория"""
        return f"https://github.com/{self.username}/{repo_name}"
    
    def delete_repository(self, repo_name: str) -> Tuple[bool, str]:
        """Удаление репозитория"""
        if not self.gh:
            return False, "GitHub токен не настроен"
        
        try:
            user = self.gh.get_user()
            repo = user.get_repo(f"{self.username}/{repo_name}")
            repo.delete()
            return True, "Репозиторий удалён"
        except Exception as e:
            return False, f"Ошибка удаления: {str(e)}"


# Глобальный экземпляр
github_automation = GitHubAutomation()