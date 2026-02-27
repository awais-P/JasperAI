import { Component } from '@angular/core';

interface MediaItem {
  id: number;
  type: 'video' | 'image';
  src: string;
  thumbnail: string;
  poster?: string;
  title: string;
  description: string;
  duration: string;
}

@Component({
  selector: 'app-home-page',
  standalone: false,
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  currentIndex = 0;

  mediaItems: MediaItem[] = [
    {
      id: 1,
      type: 'video',
      src: 'assets/media/demo-video-1.mp4',
      thumbnail: 'assets/media/thumb-1.jpg',
      poster: 'assets/media/poster-1.jpg',
      title: 'Getting Started with JasperAI',
      description: 'Learn how to set up your first automated report in under 5 minutes.',
      duration: '5:32'
    },
    {
      id: 2,
      type: 'video',
      src: 'assets/media/demo-video-2.mp4',
      thumbnail: 'assets/media/thumb-2.jpg',
      poster: 'assets/media/poster-2.jpg',
      title: 'Advanced Report Templates',
      description: 'Discover powerful template features for complex business reports.',
      duration: '8:15'
    },
    {
      id: 3,
      type: 'image',
      src: 'assets/media/feature-3.jpg',
      thumbnail: 'assets/media/thumb-3.jpg',
      title: 'Dashboard Overview',
      description: 'A comprehensive look at the JasperAI analytics dashboard.',
      duration: 'Image'
    },
    {
      id: 4,
      type: 'video',
      src: 'assets/media/demo-video-4.mp4',
      thumbnail: 'assets/media/thumb-4.jpg',
      poster: 'assets/media/poster-4.jpg',
      title: 'API Integration Guide',
      description: 'Seamlessly integrate JasperAI with your existing workflow.',
      duration: '6:45'
    },
    {
      id: 5,
      type: 'image',
      src: 'assets/media/feature-5.jpg',
      thumbnail: 'assets/media/thumb-5.jpg',
      title: 'Export Options',
      description: 'Multiple export formats including PDF, Excel, and more.',
      duration: 'Image'
    }
  ];

  nextMedia(): void {
    if (this.currentIndex < this.mediaItems.length - 1) {
      this.currentIndex++;
    }
  }

  prevMedia(): void {
    if (this.currentIndex > 0) {
      this.currentIndex--;
    }
  }

  goToMedia(index: number): void {
    this.currentIndex = index;
  }
}
